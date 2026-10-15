// Pekostrap flag bot. Receives a flag request from the app and opens a GitHub issue for review.
// The GitHub token is stored as a Worker secret (GITHUB_TOKEN); it never reaches the app.

const REPO = "7gups/Octifine";
const FLAG = /^[A-Za-z0-9_]{3,100}$/;
const MAX_BYTES = 4000;
const GROUPS = ["Rendering", "Geometry", "Physics", "Networking", "Interface", "Other"];

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ error: "method not allowed" }, 405);
    const type = (request.headers.get("content-type") || "").split(";")[0].trim();
    if (type !== "application/json") return json({ error: "content type must be application/json" }, 415);

    const text = await request.text();
    if (text.length > MAX_BYTES) return json({ error: "request too large" }, 413);

    let body;
    try {
      body = JSON.parse(text);
    } catch {
      return json({ error: "invalid JSON" }, 400);
    }

    const flag = String(body.flag ?? "").trim();
    const value = String(body.value ?? "").trim().slice(0, 200);
    const reason = String(body.reason ?? "").trim().slice(0, 2000);
    const group = String(body.group ?? "Other");
    if (!FLAG.test(flag)) return json({ error: "flag name must be 3 to 100 letters, numbers, or underscores" }, 400);
    if (reason.length < 10) return json({ error: "reason must be at least 10 characters" }, 400);
    if (!GROUPS.includes(group)) return json({ error: "unknown group" }, 400);

    // Setup details come from the app. They are free text, so keep them short and plain.
    const setup = [
      ["Pekostrap", body.pekostrapVersion], ["Android", body.androidVersion], ["Device", body.device],
      ["Pekora app", body.pekoraVersion], ["Patch method", body.patchMethod],
    ].map(([k, v]) => `- ${k}: ${String(v ?? "unknown").replace(/[\r\n`]/g, " ").slice(0, 80)}`).join("\n");

    // Same sections as the flag_request issue form, so reviewers see one format.
    const issueBody = [
      "### Flag name", "", "`" + flag + "`", "",
      "### Value", "", value || "_No value given_", "",
      "### Group", "", group, "",
      "### Why it helps", "", reason, "",
      "### Setup", "", setup, "",
      "---",
      "_Submitted from the Pekostrap app through the flag bot. Check the flag before adding it to the supported list._",
    ].join("\n");

    const res = await fetch(`https://api.github.com/repos/${REPO}/issues`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${env.GITHUB_TOKEN}`,
        Accept: "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json",
        "User-Agent": "pekostrap-flag-bot",
      },
      body: JSON.stringify({ title: `Flag request [${group}]: ${flag}`, body: issueBody, labels: ["flag-request"] }),
    });
    if (!res.ok) return json({ error: "GitHub rejected the request" }, 502);

    const issue = await res.json();
    return json({ url: issue.html_url }, 201);
  },
};

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
}
