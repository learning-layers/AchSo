Ach So!
=======

**Android video recording and annotation app for construction.**

- [Download from the Play Store.][play]
- [Project description on the Learning Layers website.][layers]

Contribution guidelines
-----------------------

> Loosely based on [Thoughbot’s Git protocol][thoughtbot].

The `master` branch should mirror the currently stable version, i.e. the one
that is currently on Google Play.

1. **Create a new branch based off `integration`** before starting work. Use a
traceable name like `feature/LL-123` or `bugfix/LL-123`, where the number
after the prefix is the issue number in JIRA. Use `feature/summary-of-feature`
if the feature doesn’t have JIRA ticket.

2. **Do your work in the new branch** and commit often with
[good commit messages][commit]. Be aware of line lengths to avoid wrapping.

3. **Check your commit history** when you’ve finished work. If you’ve got many
related commits, use interactive rebase to squash them into commits that are
cohesive and valuable.

4. **Create a pull request against `integration`** when your commits are
clean. If your pull request is mistargeted, against `master` for example,
close it and create a new one. Mark the JIRA issue as `Resolved`.

5. **Delete your working branch** after your changes have been merged back
into `integration`. The JIRA issue should be `Closed` now.

When the changes in the `integration` branch are deemed stable, they will be
merged into `master` and a new version will be released.

Versioning
----------

Since v0.10.0, Ach So releases follow [semantic versioning][semver].

Usage with Android Studio
-------------------------

1. Clone the repository
2. File -> Import Project...
3. Select the repository folder

Licence
-------

```
Copyright 2013 Aalto University, see AUTHORS

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[play]: https://play.google.com/store/apps/details?id=fi.aalto.legroup.achso
[layers]: http://developer.learning-layers.eu/tools/ach-so/
[thoughtbot]: https://github.com/thoughtbot/guides/tree/master/protocol/git
[commit]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[semver]: http://semver.org/
