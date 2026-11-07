# Repo rules

## CI: only one run at a time

After every `git push` to `xu-bu/JetMelo`, check GitHub Actions and make sure only the newest run is active:

1. List active runs (use the `xu-bu` account, not the default `gh` account):
   ```bash
   export GH_TOKEN=$(gh auth token --hostname github.com --user xu-bu)
   gh run list -R xu-bu/JetMelo --limit 10 --json databaseId,headSha,status \
     --jq '.[] | select(.status=="in_progress" or .status=="queued")'
   ```
2. Cancel every active run except the one for the commit you just pushed:
   ```bash
   gh run cancel <databaseId> -R xu-bu/JetMelo
   ```
3. Confirm exactly one run is still active, then tell the user its status.

The workflow also sets `concurrency.cancel-in-progress` in `.github/workflows/build-release.yml`, so GitHub cancels superseded runs on its own. The manual check above is a safety net.
