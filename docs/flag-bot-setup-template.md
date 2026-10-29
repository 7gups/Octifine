# Flag bot setup template

The flag bot is a small Cloudflare Worker. The app sends it a flag request, and the worker opens a GitHub issue labelled `flag-request` for review. The GitHub token stays on the worker, so the app never sees it.

The app's endpoint is set in `FlagRequests.ENDPOINT` (`android/app/src/main/java/com/voidstrap/android/FlagRequests.java`). It is empty until the bot is deployed, and the app shows "not set up" in the meantime.

## Before you start

- [ ] A Cloudflare account (free plan).
- [ ] A GitHub fine-grained token for `<owner>/<repo>` with **Issues: Read and write** only, and an expiry date.
- [ ] The `flag-request` label created in the repo (Issues → Labels).
- [ ] The issue form `.github/ISSUE_TEMPLATE/flag_request.yml` present in the repo.

## 1. Create the worker

Cloudflare's CLI (`wrangler`) doesn't run on Android, so create the worker in the dashboard:

1. Go to **Workers & Pages → Create → Create Worker**. Name it `<worker-name>`.
2. Click **Edit code**, replace the sample code with the bot code, and deploy.
3. Under **Settings → Variables and Secrets**, add a secret. Secret names can't start with `GITHUB_`, so use a name like `<SECRET_NAME>`. Paste the GitHub token as its value.
4. Make sure the worker's code reads the same secret name, for example `env.<SECRET_NAME>`.
5. Deploy again so the secret is picked up.

## 2. Connect the app

1. Copy the worker URL, `https://<worker-name>.<subdomain>.workers.dev`.
2. Set it as `ENDPOINT` in `FlagRequests.java`.
3. Rebuild the app.

## 3. Test it

Send a test request from a terminal:

```
curl -s -X POST "https://<worker-name>.<subdomain>.workers.dev" -H "Content-Type: application/json" -d '{"flag":"TestFlag","value":"true","group":"Other","reason":"Testing the flag bot setup"}'
```

A new issue labelled `flag-request` should appear. Close it afterwards.

## 4. Limit abuse

- Add a rate limit in the Cloudflare dashboard under **Security → WAF → Rate limiting rules**, for example 5 requests per minute per IP.
- Review the issues the bot opens, and close spam.

## Placeholders

| Placeholder | Meaning |
|---|---|
| `<owner>/<repo>` | The GitHub repository, for example `7gups/Octifine` |
| `<worker-name>` | The name of the Cloudflare Worker |
| `<subdomain>` | Your `workers.dev` subdomain |
| `<SECRET_NAME>` | The secret that holds the GitHub token, without the `GITHUB_` prefix |
