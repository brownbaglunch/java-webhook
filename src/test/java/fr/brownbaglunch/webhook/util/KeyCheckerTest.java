/*
 * Licensed to Brownbaglunch.fr under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Brownbaglunch.fr licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.brownbaglunch.webhook.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyCheckerTest {

    @Test
    void testKey() {
        String json = "{\"ref\":\"refs/heads/gh-pages\",\"before\":\"24b3a3b4c1570b325f6365453bcfa293e44135a4\"," +
                "\"after\":\"d43f31303388e71540ebbb0f3bc863fc9e1787e0\",\"created\":false,\"deleted\":false,\"forced\":false," +
                "\"base_ref\":null,\"compare\":\"https://github.com/brownbaglunch/bblfr_data/compare/24b3a3b4c157...d43f31303388\"," +
                "\"commits\":[{\"id\":\"d43f31303388e71540ebbb0f3bc863fc9e1787e0\"," +
                "\"tree_id\":\"ae2c87c2d5599ebfe08097abe2b61fe7f56380b9\",\"distinct\":true,\"message\":\"Add missing city Amiens\"," +
                "\"timestamp\":\"2018-01-16T12:27:01+01:00\",\"url\":\"https://github" +
                ".com/brownbaglunch/bblfr_data/commit/d43f31303388e71540ebbb0f3bc863fc9e1787e0\",\"author\":{\"name\":\"David Pilato\"," +
                "\"email\":\"foo@bar.com\",\"username\":\"dadoonet\"},\"committer\":{\"name\":\"David Pilato\"," +
                "\"email\":\"foo@bar.com\",\"username\":\"dadoonet\"},\"added\":[],\"removed\":[],\"modified\":[\"baggers.js\"]}]," +
                "\"head_commit\":{\"id\":\"d43f31303388e71540ebbb0f3bc863fc9e1787e0\"," +
                "\"tree_id\":\"ae2c87c2d5599ebfe08097abe2b61fe7f56380b9\",\"distinct\":true,\"message\":\"Add missing city Amiens\"," +
                "\"timestamp\":\"2018-01-16T12:27:01+01:00\",\"url\":\"https://github" +
                ".com/brownbaglunch/bblfr_data/commit/d43f31303388e71540ebbb0f3bc863fc9e1787e0\",\"author\":{\"name\":\"David Pilato\"," +
                "\"email\":\"foo@bar.com\",\"username\":\"dadoonet\"},\"committer\":{\"name\":\"David Pilato\"," +
                "\"email\":\"foo@bar.com\",\"username\":\"dadoonet\"},\"added\":[],\"removed\":[],\"modified\":[\"baggers.js\"]}," +
                "\"repository\":{\"id\":35821731,\"name\":\"bblfr_data\",\"full_name\":\"brownbaglunch/bblfr_data\"," +
                "\"owner\":{\"name\":\"brownbaglunch\",\"email\":\"foo@bar.com\",\"login\":\"brownbaglunch\",\"id\":12496974," +
                "\"avatar_url\":\"https://avatars3.githubusercontent.com/u/12496974?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api" +
                ".github.com/users/brownbaglunch\",\"html_url\":\"https://github.com/brownbaglunch\",\"followers_url\":\"https://api" +
                ".github.com/users/brownbaglunch/followers\",\"following_url\":\"https://api.github" +
                ".com/users/brownbaglunch/following{/other_user}\",\"gists_url\":\"https://api.github" +
                ".com/users/brownbaglunch/gists{/gist_id}\",\"starred_url\":\"https://api.github" +
                ".com/users/brownbaglunch/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github" +
                ".com/users/brownbaglunch/subscriptions\",\"organizations_url\":\"https://api.github.com/users/brownbaglunch/orgs\"," +
                "\"repos_url\":\"https://api.github.com/users/brownbaglunch/repos\",\"events_url\":\"https://api.github" +
                ".com/users/brownbaglunch/events{/privacy}\",\"received_events_url\":\"https://api.github" +
                ".com/users/brownbaglunch/received_events\",\"type\":\"Organization\",\"site_admin\":false},\"private\":false," +
                "\"html_url\":\"https://github.com/brownbaglunch/bblfr_data\",\"description\":\"Brown Bag Lunch baggers database\"," +
                "\"fork\":false,\"url\":\"https://github.com/brownbaglunch/bblfr_data\",\"forks_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/forks\",\"keys_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/keys{/key_id}\",\"collaborators_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/collaborators{/collaborator}\",\"teams_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/teams\",\"hooks_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/hooks\",\"issue_events_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/issues/events{/number}\",\"events_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/events\",\"assignees_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/assignees{/user}\",\"branches_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/branches{/branch}\",\"tags_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/tags\",\"blobs_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/git/blobs{/sha}\",\"git_tags_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/git/tags{/sha}\",\"git_refs_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/git/refs{/sha}\",\"trees_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/git/trees{/sha}\",\"statuses_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/statuses/{sha}\",\"languages_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/languages\",\"stargazers_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/stargazers\",\"contributors_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/contributors\",\"subscribers_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/subscribers\",\"subscription_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/subscription\",\"commits_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/commits{/sha}\",\"git_commits_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/git/commits{/sha}\",\"comments_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/comments{/number}\",\"issue_comment_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/issues/comments{/number}\",\"contents_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/contents/{+path}\",\"compare_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/compare/{base}...{head}\",\"merges_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/merges\",\"archive_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/{archive_format}{/ref}\",\"downloads_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/downloads\",\"issues_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/issues{/number}\",\"pulls_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/pulls{/number}\",\"milestones_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/milestones{/number}\",\"notifications_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/notifications{?since,all,participating}\",\"labels_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/labels{/name}\",\"releases_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/releases{/id}\",\"deployments_url\":\"https://api.github" +
                ".com/repos/brownbaglunch/bblfr_data/deployments\",\"created_at\":1431958287,\"updated_at\":\"2017-12-21T11:06:39Z\"," +
                "\"pushed_at\":1516102032,\"git_url\":\"git://github.com/brownbaglunch/bblfr_data.git\",\"ssh_url\":\"git@github" +
                ".com:brownbaglunch/bblfr_data.git\",\"clone_url\":\"https://github.com/brownbaglunch/bblfr_data.git\"," +
                "\"svn_url\":\"https://github.com/brownbaglunch/bblfr_data\",\"homepage\":\"http://www.brownbaglunch.fr/baggers.html\"," +
                "\"size\":3198,\"stargazers_count\":24,\"watchers_count\":24,\"language\":\"JavaScript\",\"has_issues\":true," +
                "\"has_projects\":true,\"has_downloads\":true,\"has_wiki\":false,\"has_pages\":true,\"forks_count\":177," +
                "\"mirror_url\":null,\"archived\":false,\"open_issues_count\":5,\"license\":{\"key\":\"apache-2.0\",\"name\":\"Apache " +
                "License 2.0\",\"spdx_id\":\"Apache-2.0\",\"url\":\"https://api.github.com/licenses/apache-2.0\"},\"forks\":177," +
                "\"open_issues\":5,\"watchers\":24,\"default_branch\":\"gh-pages\",\"stargazers\":24,\"master_branch\":\"gh-pages\"," +
                "\"organization\":\"brownbaglunch\"},\"pusher\":{\"name\":\"dadoonet\",\"email\":\"foo@bar.com\"}," +
                "\"organization\":{\"login\":\"brownbaglunch\",\"id\":12496974,\"url\":\"https://api.github.com/orgs/brownbaglunch\"," +
                "\"repos_url\":\"https://api.github.com/orgs/brownbaglunch/repos\",\"events_url\":\"https://api.github" +
                ".com/orgs/brownbaglunch/events\",\"hooks_url\":\"https://api.github.com/orgs/brownbaglunch/hooks\"," +
                "\"issues_url\":\"https://api.github.com/orgs/brownbaglunch/issues\",\"members_url\":\"https://api.github" +
                ".com/orgs/brownbaglunch/members{/member}\",\"public_members_url\":\"https://api.github" +
                ".com/orgs/brownbaglunch/public_members{/member}\",\"avatar_url\":\"https://avatars3.githubusercontent" +
                ".com/u/12496974?v=4\",\"description\":\"BBLFR Github Repositories\"},\"sender\":{\"login\":\"dadoonet\",\"id\":274222," +
                "\"avatar_url\":\"https://avatars3.githubusercontent.com/u/274222?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github" +
                ".com/users/dadoonet\",\"html_url\":\"https://github.com/dadoonet\",\"followers_url\":\"https://api.github" +
                ".com/users/dadoonet/followers\",\"following_url\":\"https://api.github.com/users/dadoonet/following{/other_user}\"," +
                "\"gists_url\":\"https://api.github.com/users/dadoonet/gists{/gist_id}\",\"starred_url\":\"https://api.github" +
                ".com/users/dadoonet/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github" +
                ".com/users/dadoonet/subscriptions\",\"organizations_url\":\"https://api.github.com/users/dadoonet/orgs\"," +
                "\"repos_url\":\"https://api.github.com/users/dadoonet/repos\",\"events_url\":\"https://api.github" +
                ".com/users/dadoonet/events{/privacy}\",\"received_events_url\":\"https://api.github" +
                ".com/users/dadoonet/received_events\",\"type\":\"User\",\"site_admin\":false}}";
        assertTrue(KeyChecker.testGithubToken(json, "sha1=6800e0f1f44f69cdd348360c0140526ff1dff852", "myFakeKey"));
        assertFalse(KeyChecker.testGithubToken(json, "sha1=6800e0f1f44f69cdd348360c0140526ff1dff852", "incorrectKey"));
        assertFalse(KeyChecker.testGithubToken(json, "sha1=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "myFakeKey"));
        assertFalse(KeyChecker.testGithubToken(json, null, "myFakeKey"));
        assertFalse(KeyChecker.testGithubToken(json, "sha1=6800e0f1f44f69cdd348360c0140526ff1dff852", null));
        assertTrue(KeyChecker.testGithubToken("", "sha1=22202b35f1482c1a8d3d0c3f6b3c46307792d409", "myFakeKey"));
    }
}
