# Release Checklist

## Release

- [ ] leverage static-serve
- [ ] user page (where member, admin, and executive)
- [ ] leverage honeysql
- [ ] multi-language
- [ ] custom terminologies (law, org, mem)
- [ ] Migrate to next.jdbc

## Beta

- [ ] delete account
- [ ] delete org
- [ ] remove org users
- [ ] user, org, const, law, ballot 404
- [ ] delete law
- [ ] delete ballot
- [ ] enable edit of org name
- [ ] enable edit of const title, description
- [ ] "your organisations"
- [ ] simplify router/post and router/page
- [ ] simplify get-sess/get-param between pages
- [ ] leverage TTL cache
- [ ] check if admin for operations asap (perhaps an auth func dict in router)
- [ ] check if org/const/poll exists asap (perhaps a 404 func dict in router)
- [ ] people -> person
- [ ] v/ user/org/const links
- [ ] homepage statistics
- [ ] design house style
- [ ] cryptographically secure salt
- [ ] leverage Clojure destructuring appropriately
- [ ] choice to always show ballot results
- [ ] choose score precision
- [ ] https://github.com/yogthos/lein-asset-minifier
- [ ] Split db into separate namespaces
- [ ] Use -? only for functions; use is- for variables

## Alpha

- [x] registration
- [x] signin
- [x] signout
- [x] user exclusive pages
- [x] user exclusive posts
- [x] create org
- [x] list orgs (with num mems)
- [x] new constitution
- [x] add members to org, as admin
- [x] view org (num members, link constitutions, link polls)
- [x] new law
- [x] view constitution (num affected members, orgs, link laws, link ballots)
- [x] new poll
- [x] Majority Approvals poll
- [x] Score Approvals poll
- [x] poll results
- [x] add member org to const
- [x] add exec org to const
- [x] extrapolate latest ballot results per law
- [ ] quorum
- [ ] law, ballot dates
- [ ] form errors
- [ ] leverage defn-
- [ ] leverage clj-time
- [ ] cache control
- [ ]
