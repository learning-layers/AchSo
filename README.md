Ach so!
=======

**Android video recording and annotation app for construction.**

- [Download from the Play Store.][play]
- [Project description on the Learning Layers website.][layers]

Usage with Android Studio
-------------------------

Please use the latest Android Studio version from the stable channel.

1. Clone the repository.
2. File → Import Project...
3. Select the `build.gradle` file in the project root directory.

Launching Ach so! from intents
------------------------------

You can launch Ach so! just to play singular Ach so!-videos. (if e.g. some other service finds Ach
 so! videos as resources and wants to show them for the user)

If you provide generic Intent.VIEW with an uri using the 'achso:'-scheme, Ach so! will
load the uri and play the video:

```java
Intent intent = new Intent(Intent.ACTION_VIEW);
// we have a video with id "2de064a5-cb38-4c04-9fde-502ab619fc46"
// uri will be "achso:2de064a5-cb38-4c04-9fde-502ab619fc46"
Uri uri = Uri.fromParts("achso", "2de064a5-cb38-4c04-9fde-502ab619fc46", null);
intent.setData(uri);
startActivity(intent);
```

Another way to show a single video is to specifically ask Ach so! to play a .mp4 -video. In this
case Ach so! looks if there are annotations available for this video file in the default server
and fetches those.

```java
Intent intent = new Intent("fi.aalto.legroup.achso.action.VIEW");
Uri uri = Uri.parse("http://www.sample-videos.com/video/mp4/720/big_buck_bunny_720p_1mb.mp4");
intent.setDataAndType(uri, "video/mp4");
startActivity(intent);
```

There are libraries to launch intents in PhoneGap/Cordova. If you want to play Ach so! videos in
non-android environments, use video player in [Achrails](https://github.com/learning-layers/achrails).


Contribution guidelines
-----------------------

> Loosely based on [Thoughtbot’s Git protocol][thoughtbot].

The `master` branch mirrors the currently stable version, i.e. the one that is
currently on Google Play. The history on this branch is final and must never be
rewritten.

The `integration` branch hosts the current development version. Rebasing this
branch is discouraged, but it can be done if there’s a good enough reason and
contributors are consulted with beforehand.

Other branches are working branches. You can expect their history to be
rewritten at any time.

1. Create a new branch based off `integration` before starting work. Use a
   traceable name like `feature/LL-123` or `bugfix/LL-123`, where the number
   after the prefix is the issue number in JIRA. Use
   `feature/summary-of-feature` if the feature doesn’t have JIRA ticket.

2. Do your work in the new branch and commit often with
   [good, descriptive commit messages][commit]. The first line of your commit
   message should not exceed 50 characters.

3. Check your commit history when you’ve finished work. Use interactive rebase
   `git rebase -i integration` to clean it up. Your commits should be atomic:
   each commit should contain one change that doesn’t depend on other commits
   to build. This is easier if you commit often and then rebase.

4. Create a pull request against `integration` when your commits are clean. If
   you accidentally mistarget the pull request, against `master` for example,
   close it and create a new one.

5. Mark the JIRA issue as `resolved`.

6. Other contributors will review your changes. If they need more work, push
   additional commits to the working branch.

7. The reviewer marks the JIRA issue as `closed` and merges the pull request.

8. Delete your working branch when it’s no longer needed. This can be done
   easily from the pull request page.

When the changes in the `integration` branch are deemed stable, they will be
merged into `master` and a new version is released.

Versioning
----------

Since v0.10.0, Ach so! releases follow [semantic versioning][semver] and each
release is tagged in the `master` branch.

Authors
-------

Ach so! is developed by the Learning Environments research group at the School
of Arts, Design and Architecture of Aalto University, Finland.

#### Development, design and direction:

- Jukka Purma (@jpurma)
- Merja Bauters
- Antti Keränen (@Detegr)
- Leo Nikkilä (@lnikkila)
- Lassi Veikkonen (@lassiveikkonen)
- Samuli Raivio (@bqqbarbhg)
- Matti Jokitulppo (@melonmanchan)
- Teemu Leinonen
- Tarmo Toikkanen (@tarmot)
- Jana Pejoska
- Kiarii Ngua
- Marjo Virnes
- Sanna Reponen

#### Contributions from the RWTH Aachen University by:

- Petru Nicolaescu
- István Koren

Licence
-------

```
Copyright 2013–2017 Aalto University

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
