## Database structure

- org (org_id, date, name, pub_con)
- user (user_id, date, email, password, salt, pub_con)
- const (const_id, date, title, desc, pub_con)
- law (law_id, const_id, parent_id, date, user_id, title, body)
- ballot (ballot_id, date, user_id, method_id, start, hours)
- option (option_id, ballot_id, law_id)
- vote (vote_id, date, option_id, voter_id, value)
- org2user (n2u_id, org_id, user_id, is_admin)
- org2const (n2c_id, org_id, const_id, is_exec)

## Vote methods in detail

### Methods

Can I mass-approve things?
Can I individually approve things?
Can I score and major on everything?
Can I Single-Transferable options?

#### Majority Approvals
Choice: approve or disapprove per option
Result: individual options approved/disapproved by majority
type: 1

#### Score Approvals
Choice: 1..5 per option
Result: individual options approved by score
type: 2

#### Most-Approvals
Choice: approve or disapprove per option
Result: N options approved by majority
type: 3, num_win: X

#### Highest-Score
Choice: 1..5 per option
Result: N options approved by score
type: 4, num_win: N

#### Single-Transferable
Choice: rank per option
Result: N options approved by transferred totals
type: 5, num_win: N

#### Mass Majority Approval
Choice: approve or disapprove of all options
Result: all options approved by majority
type: 6

#### Mass Score Approval
Choice: 1..5 of all options
Result: all options approved by score
type: 7




#### Law Majority Approval
IsLaw, IsMajority, IsApprove, !IsMass
Laws:	N
Choice:	1 of N laws
Result:	N laws individually approved by majority

ballot	{ type: 1, num_win: N }
option:	{ ballot_id, law_id }
vote:   { option_id, voter_id, value: bool }

#### Law Score Approval
IsLaw, !IsMajority, IsApprove, !IsMass
Laws:	N
Choice:	1..5 approval per law
Result:	N laws individually approved by average

ballot	{ type: 2, num_win: N }
option:	{ ballot_id, law_id }
vote:   { option_id, voter_id, value: 1..5 }

#### Law Single-Transferable
IsLaw, !IsMajority, !IsApprove, !IsMass
Laws:	N
Choice:	N laws ranked by approval
Result:	top X approved by transferred totals

ballot  { type: 3, num_win: X }
option:	{ ballot_id, law_id }
vote:   { option_id, voter_id, law_id, value: 1.. }

#### Laws Majority Approval
IsLaw, IsMajority, IsApprove, IsMass
Laws: N
Choice: approval of all N laws
Result: total N approval by majority

ballot:	{ type: 4, num_win: N }
option:	{ ballot_id, law_id }
vote:	{ option_id, voter_id, law_id, value: bool }

#### Laws Score Approval
IsLaw, !IsMajority, IsApprove, IsMass
Laws: N
Choice: approval of all N laws
Result: total N approval by average

ballot:	{ type: 4, num_win: N }
option:	{ ballot_id, law_id }
vote:	{ option_id, voter_id, law_id, value: bool }

#### Option Majority Approval
!IsLaw, IsMajority, IsApprove, !IsMass
Laws: 0
Choice: Approve or Disapprove of N options
Result: X options approved by majority

ballot: { type: 5, num_win: X }
option: { ballot_id, text: option }
vote { option_id, voter_id, value: bool }

#### Option Score Approval
!IsLaw, !IsMajority, IsApprove, !IsMass
Laws: 0
Choice: 1..5 approval per option
Result: X options approved by average

ballot: { type: 6, num_win: X }
option: { ballot_id, text: option }
vote { option_id, voter_id, value: 1..5 }

#### Option Single-Transferable
!IsLaw, !IsMajority, !IsApprove, !IsMass
Laws:	0
Choice:	N options ranked by approval
Result:	top X approved by transferred totals

ballot  { type: 7, num_win: X }
option:	{ ballot_id, text: option }
vote:   { option_id, voter_id, value: 1.. }

### Old

**RDB tables**

- org (org_id, date, name, pub_con)
- user (user_id, date, email, password, salt, pub_con)
- law (law_id, date, user_id, title, body)
- const (const_id, date, title, desc, pub_con)
- ballot (ballot_id, date, user_id, law_id, type, start, hours)
- vote (vote_id, date, voter_id, ballot_id, value)
- option (option_id, ballot_id, text)
- org2user (n2u_id, org_id, user_id, is_admin)
- org2const (n2c_id, org_id, const_id, is_exec)
- law2const (l2c_id, law_id, const_id, top_law_id)
- vote2option (v2o_id, vote_id, option_id, quantity)

**Vote types**

- numeric vote(value)
- one option vote2option
- many options vote2option
- quantity of options vote2option(quantity)

**Views**
- home (stats)
- user (user)
- organisation (org, org2user:user, org2const:const)
- law (law, votes)
- constitution (laws)
- ballot (ballot, law)

