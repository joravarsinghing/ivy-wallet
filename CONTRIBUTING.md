# Contributing to Jvy Wallet

>[!IMPORTANT]
>**Jvy Wallet is actively maintained**
>
>Jvy Wallet is an independent continuation of the open-source Ivy Wallet codebase.
>
>Pull requests, issues, and thoughtful product feedback are welcome. Existing Ivy Wallet import compatibility must be preserved.

For licensing terms and the project disclaimer, see [LICENSE](LICENSE).


## 1. Prepare your workspace

Clone or fork this repository, then create a feature branch from the branch requested by the issue or roadmap packet.


## 2. Pick an issue
### Workflow:
1. Browse the issues in this repository.
2. Choose an issue that you understand and like.
> Didn't find anything fitting? Create a new issue with a clear problem statement and expected behavior.
3. If the issue is already assigned, coordinate before starting overlapping work.

### Contributing rules:
1. Do **not** work on already assigned issues without agreement from the assignee.
2. Keep work focused and communicate when scope changes.
3. Preserve Ivy Wallet import compatibility and avoid changing financial semantics without tests and documentation.


## 3. Create a feature branch
Create a focused branch in your `ivy-wallet` folder:
```
git checkout -b fix-issue-{YOUR_ISSUE_NUMBER}
```
> Replace {YOUR_ISSUE_NUMBER} with the ID/number of your issue.


## 4. Time to work
### ⚠️ Very important - read the [Developer Guidelines 🏗️](docs/Guidelines.md) before you begin.

### Workflow:
- Make commits.
- Refactor your code.
- Verify that your implementation works.
- Build often and test that you haven't broken existing features.

### Tips:
- Make sure that you don't break anything with your changes.
- Keep it simple.
- "Don't walk away from complexity, run!"

### Ask Yourself:
- Is that the simplest solution?
- Can I do it with less code and changes?
- Does it work in all cases?


## 5. Submit a pull request
So far, you should have pushed your work to your feature branch and have tested
that it works on a real Android device when the change affects UI or device behavior.
The final step is to open a pull request in this repository against the target branch named by the roadmap or issue.

**[Submitting a PR - GitHub tutorial](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork)**

### IMPORTANT:
- Make sure the base branch matches the active roadmap or issue.
- Include a concise summary, screenshots for UI changes, and the tests you ran.
- Jvy Wallet is a small project, so **you are the QA!** Please test your implementation carefully.

### Questions?
Open a GitHub issue or use the contact links in [README.md](README.md).
