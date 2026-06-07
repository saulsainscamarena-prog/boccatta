# Plan: Extract Hardcoded Strings

## 1. Milestones
1. Write the extraction script.
2. Review the script.
3. Run the script and verify the changes.

## 2. Testing Strategy
- Run the script on a subset of files or print the proposed changes first (dry run mode).
- Compile the app after applying the changes.
- Verify UI visually.

## 3. Deployment
- The script is a one-time utility.

## 4. Rollback
- Since changes will be tracked in git, rollback is as simple as `git reset --hard` if the script produces incorrect results.
