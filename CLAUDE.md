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

## Compile after every code change

After writing or editing code, compile locally before reporting it as done. Use JDK 21. The user prefers the newest local JDK in general, but Gradle 8.11.1 / AGP 8.9.1 do not support Android Studio's JDK 25 (the build hung), so the user chose JDK 21 for this project. Do not switch JDKs without asking. Run in the foreground with a time limit, and do not use `-q` (so a hang is visible):

```bash
export JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.7+6/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
sh ./gradlew :app:compileDebugKotlin --console=plain
```

Fix every compile error before moving on. If the compile cannot run, say so and why instead of reporting the change as verified.

**Exception: when the user says "push", commit and push right away.** Do not wait for a running or pending local compile to finish. Start the compile in the background if none is running, push, and report the compile result when it arrives (CI also builds every push).
