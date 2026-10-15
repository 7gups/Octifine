# Pekostrap flag bot

Receives flag requests from the app and opens a GitHub issue with the `flag-request` label.
The GitHub token stays on the server. The app only sends the flag name, value, and reason.

## Set up

1. Create a fine-grained GitHub token for `7gups/Octifine` with **Issues: Read and write** only, and set an expiry.
2. Create the `flag-request` label in the repo (Issues → Labels).
3. Deploy (needs a free Cloudflare account and Node.js):

   ```
   cd flag-bot
   npx wrangler login
   npx wrangler secret put GITHUB_TOKEN   # paste the token
   npx wrangler deploy
   ```

4. Copy the `https://flag-bot.<name>.workers.dev` URL into `FlagRequests.ENDPOINT` in the app, then rebuild.

## Limit abuse

Anyone can send a request to the endpoint, so add a rate limit in the Cloudflare dashboard
(Security → WAF → Rate limiting rules), for example 5 requests per minute per IP on the worker route.
Review the issues the bot opens, and close spam.
