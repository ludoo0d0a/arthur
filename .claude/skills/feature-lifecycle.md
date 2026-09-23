# Feature Lifecycle

This skill defines the workflow for implementing features to ensure they are tracked, automated, and cleaned up.

## Workflow

1. **Branching**: Create a new feature branch.
2. **Implementation**: Implement the feature and verify it with tests.
3. **PR Creation**: Create a Pull Request using `gh pr create`. Include "closes #ID" in the body to ensure the linked issue is closed upon merge.
4. **Auto-Merge**: Enable auto-merge for the PR using `mcp__ccd_pr__set_auto_merge`.
5. **Auto-Archive**: Set the session monitor to auto-archive on close using `mcp__ccd_pr__set_monitor` with `auto_archive_on_close: true`.
