#!/usr/bin/env python3
"""Draws a star-history chart for a GitHub repository as light and dark SVGs.

Standard library only. Usage:
    python3 star_chart.py --repo 7gups/Octifine --out dist
Set GITHUB_TOKEN for a higher API rate limit (optional).
"""
import argparse
import datetime as dt
import json
import math
import os
import urllib.request
from xml.sax.saxutils import escape

API = "https://api.github.com/repos/{repo}/stargazers?per_page=100&page={page}"
MAX_PAGES = 100  # 10,000 stars; enough for this project


def fetch_star_dates(repo, token=None):
    """Returns the starred_at timestamps of every stargazer, oldest first."""
    dates = []
    for page in range(1, MAX_PAGES + 1):
        req = urllib.request.Request(API.format(repo=repo, page=page), headers={
            "Accept": "application/vnd.github.star+json",
            "User-Agent": "octifine-star-chart",
        })
        if token:
            req.add_header("Authorization", "Bearer " + token)
        with urllib.request.urlopen(req, timeout=30) as r:
            batch = json.loads(r.read().decode("utf-8"))
        if not batch:
            break
        dates.extend(item["starred_at"] for item in batch)
        if len(batch) < 100:
            break
    dates.sort()
    return [dt.datetime.strptime(d, "%Y-%m-%dT%H:%M:%SZ").date() for d in dates]


def cumulative_by_day(star_days, today):
    """One (day, total) point per day from the first star until today."""
    if not star_days:
        return [(today, 0)]
    first = star_days[0]
    counts = {}
    for d in star_days:
        counts[d] = counts.get(d, 0) + 1
    points, total, day = [], 0, first
    while day <= today:
        total += counts.get(day, 0)
        points.append((day, total))
        day += dt.timedelta(days=1)
    return points


def nice_max(value):
    """Top of the y-axis: a multiple of 4 with a nice step, so the four gridline ticks are evenly spaced."""
    if value <= 4:
        return 4
    step_needed = math.ceil(value / 4)
    magnitude = 10 ** math.floor(math.log10(step_needed))
    for m in (1, 2, 5, 10):
        if m * magnitude >= step_needed:
            return 4 * m * magnitude


THEMES = {
    "light": {"bg": "#ffffff", "fg": "#24292f", "muted": "#57606a", "grid": "#d0d7de", "line": "#8a2be2", "fill": "#8a2be222"},
    "dark": {"bg": "#0d1117", "fg": "#e6edf3", "muted": "#8b949e", "grid": "#30363d", "line": "#b48cff", "fill": "#b48cff33"},
}


def render(points, repo, theme, width=720, height=360):
    t = THEMES[theme]
    left, right, top, bottom = 56, 24, 56, 40
    plot_w, plot_h = width - left - right, height - top - bottom
    first, last = points[0][0], points[-1][0]
    span = max((last - first).days, 1)
    top_value = nice_max(max(v for _, v in points))

    def x(day):
        return left + plot_w * (day - first).days / span

    def y(value):
        return top + plot_h * (1 - value / top_value)

    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" font-family="system-ui, sans-serif">',
           f'<rect width="{width}" height="{height}" fill="{t["bg"]}"/>',
           f'<text x="{left}" y="28" font-size="16" font-weight="600" fill="{t["fg"]}">{escape(repo)} stars</text>',
           f'<text x="{width - right}" y="28" font-size="14" text-anchor="end" fill="{t["muted"]}">{points[-1][1]} stars</text>']
    for i in range(5):
        value = top_value * i // 4
        yy = y(value)
        out.append(f'<line x1="{left}" y1="{yy:.1f}" x2="{width - right}" y2="{yy:.1f}" stroke="{t["grid"]}" stroke-width="1"/>')
        out.append(f'<text x="{left - 8}" y="{yy + 4:.1f}" font-size="11" text-anchor="end" fill="{t["muted"]}">{value}</text>')
    # month labels at the first point of each month
    seen = set()
    for day, _ in points:
        key = (day.year, day.month)
        if key in seen:
            continue
        seen.add(key)
        out.append(f'<text x="{x(day):.1f}" y="{height - 14}" font-size="11" fill="{t["muted"]}">{day.strftime("%b %Y")}</text>')
    coords = [(x(d), y(v)) for d, v in points]
    line = " ".join(f"{px:.1f},{py:.1f}" for px, py in coords)
    area = f"{left},{top + plot_h} " + line + f" {width - right},{top + plot_h}"
    out.append(f'<polygon points="{area}" fill="{t["fill"]}"/>')
    out.append(f'<polyline points="{line}" fill="none" stroke="{t["line"]}" stroke-width="2.5" stroke-linejoin="round"/>')
    out.append('</svg>')
    return "\n".join(out) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True)
    parser.add_argument("--out", default="dist")
    args = parser.parse_args()
    today = dt.datetime.now(dt.timezone.utc).date()
    star_days = fetch_star_dates(args.repo, os.environ.get("GITHUB_TOKEN"))
    points = cumulative_by_day(star_days, today)
    os.makedirs(args.out, exist_ok=True)
    for theme in THEMES:
        path = os.path.join(args.out, f"star-chart-{theme}.svg")
        with open(path, "w", encoding="utf-8") as f:
            f.write(render(points, args.repo, theme))
        print("wrote", path)
    print(f"{len(star_days)} stars, {len(points)} days")


if __name__ == "__main__":
    main()
