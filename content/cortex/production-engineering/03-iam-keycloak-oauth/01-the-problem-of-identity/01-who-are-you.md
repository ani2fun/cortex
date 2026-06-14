---
title: '1. Who are you, and why should I believe you?'
summary: Authentication answers "who are you?"; authorization answers "what are you allowed to do?". They are different questions, they fail in different ways, and confusing them is how systems get breached.
---

# 1. Who are you, and why should I believe you?

## TL;DR

> Every protected system asks two separate questions. **Authentication** ("authN") is *"who are you, and can you prove it?"* **Authorization** ("authZ") is *"now that I know who you are, what are you allowed to do?"* They are not the same question, they are answered by different machinery, and almost every famous breach is a story about one of them failing — usually because someone treated them as one thing.

## 1. Motivation

In August 2012, a journalist named Mat Honan watched his entire digital life evaporate in under an hour. Attackers took over his Twitter, wiped his iPhone, his iPad, and his MacBook, and deleted eight years of email and the only photos of his daughter's first year.

They never guessed a single password.

Here is how they did it. Amazon let them *add* a new credit card to Honan's account using only his billing address and email — information that is practically public. Then they called Amazon back and said "I've lost access to my account," and Amazon verified the caller by asking for… a credit card number on file. The one they had just added. Now inside the Amazon account, they could see the last four digits of his *real* cards. Apple's support line, at the time, would verify a caller's identity using a billing address and the last four digits of a credit card. Those last four digits unlocked an Apple ID reset, and the Apple ID was the master key to every Apple device he owned.

No system in that chain was "hacked" in the movie sense. Each one simply **mistook a piece of information for a proof of identity**. The attack was a hot knife sliding between *authentication* (proving who you are) and the assumption that knowing a fact about someone is the same as being them.

That seam — the gap between "I have a fact about you" and "I am you" — is the entire subject of this Part.

## 2. Intuition (Analogy)

Think about getting into a nightclub.

At the door, a bouncer checks your **ID**. They are answering one question: *are you really the person you claim to be, and are you old enough?* That is **authentication**. The bouncer doesn't care yet whether you're allowed in the VIP room — they just want to know your identity is real.

Inside, there's a second system. A **wristband**. Green means general floor; gold means VIP; a stamp on your hand means you've paid and can come and go. The wristband doesn't re-check who you are — the door already did that. It encodes *what you're allowed to do*. That is **authorization**.

| The club | A software system |
|---|---|
| Showing your ID at the door | Authentication — *proving identity* |
| The bouncer trusting the ID is genuine | Verifying a credential (password, token signature) |
| The wristband color | Authorization — *what you may do* |
| "VIP only" rope | An access-control check on a protected action |
| A forged ID that looks real | A stolen password or forged token |
| A real ID, but you're not on the VIP list | Authenticated, but not authorized |

Two doors, two questions. The bouncer and the wristband are *different mechanisms*, and a well-run club never lets one impersonate the other. The Honan attack worked because Amazon and Apple let a wristband (a fact on file) act like an ID at the door.

## 3. Formal Definition

Let's nail the vocabulary, because the rest of this Part leans on it.

| Term | Definition | In Cortex |
|---|---|---|
| **Principal** | The entity making a request — a human user, or another program. | The signed-in reader; an automated script hitting the API. |
| **Identity** | The claim of *who* a principal is. | "I am the GitHub user `ani2fun`." |
| **Credential** | The evidence offered to back an identity claim. | A password, a one-time code, a cryptographically signed token. |
| **Authentication (authN)** | The process of verifying a credential and accepting (or rejecting) an identity claim. | Keycloak checking your GitHub login, then issuing a token. |
| **Authorization (authZ)** | The process of deciding whether an *authenticated* principal may perform a specific action on a specific resource. | "This user is on the submission allow-list, so they may POST a solution." |
| **Factor** | A *category* of evidence. Three exist: something you **know** (password), something you **have** (phone, security key), something you **are** (fingerprint). | Cortex delegates this to GitHub/Keycloak. |

The single most important sentence in this chapter:

> **Authentication establishes identity. Authorization uses that identity to make a decision. You must do them in that order, and you must never let the second one quietly stand in for the first.**

A system that checks "is this request carrying *a* valid token?" but never "is this the *right* principal for *this* resource?" has authenticated without authorizing — the classic broken-access-control bug. A system that grants access because a request *knows a secret fact* (a card number, a user id in the URL) without proving the principal *is* that user has authorized without authenticating — the Honan failure.

## 4. Worked Example

Walk through what must happen, conceptually, when you click **Edit** on a code block on this very site.

```d2
direction: right

reader: Reader's browser {
  shape: person
}
authn: "AuthN: who are you?" {
  shape: hexagon
}
authz: "AuthZ: may you edit?" {
  shape: hexagon
}
allow: Edit unlocked {
  shape: oval
}
deny: Sign-in prompt {
  shape: oval
}

reader -> authn: "click Edit"
authn -> authz: "identity = GitHub user X" {style.stroke-dash: 0}
authn -> deny: "no valid identity"
authz -> allow: "X is allowed"
authz -> deny: "X not allowed"
```

1. **Authentication first.** The system asks: is this request carrying proof of a real identity? If you're signed in, your browser holds a token that says "this is GitHub user X, vouched for by Keycloak." If not, there is no identity to reason about — straight to the sign-in prompt.
2. **Authorization second.** *Given* that you are X, are you permitted to edit and run code? Cortex's rule (which we'll meet in detail later) is "anyone signed in may edit." A different rule — "only users on the submission allow-list may submit solutions" — is a *second, separate* authorization check on a *different* action.

Notice the order is not negotiable. You cannot ask "may you edit?" until you know *who* is asking. And answering "who are you?" tells you nothing, by itself, about what you're allowed to do.

## 5. Build It

Run this. It models the two questions as two functions and shows how a system that collapses them into one gets fooled.

```python run
# A deliberately tiny identity system. The bug is the point.

USERS = {
    # username -> password (NEVER store plaintext passwords in real life; chapter 3 explains why)
    "ada":  "lovelace",
    "alan": "turing",
}

# Authorization facts — who may do what — kept SEPARATE from credentials.
MAY_PUBLISH = {"ada"}  # only Ada may publish


def authenticate(username, password):
    """AuthN: prove identity. Returns the principal, or None."""
    if USERS.get(username) == password:
        return username
    return None


def authorize(principal, action):
    """AuthZ: decide on an action for an ALREADY-authenticated principal."""
    if action == "publish":
        return principal in MAY_PUBLISH
    return True  # everything else is allowed to any signed-in user


def secure_endpoint(username, password, action):
    principal = authenticate(username, password)
    if principal is None:
        return "401 Unauthorized — who are you?"
    if not authorize(principal, action):
        return f"403 Forbidden — {principal} may not {action}"
    return f"200 OK — {principal} did {action}"


print(secure_endpoint("ada",  "lovelace", "publish"))   # authN ok, authZ ok
print(secure_endpoint("alan", "turing",   "publish"))   # authN ok, authZ DENIED
print(secure_endpoint("eve",  "guess",    "read"))      # authN fails first


# ---- Now the Honan-style bug: authorize WITHOUT authenticating ----
def broken_endpoint(claimed_username, action):
    # "They knew the username, so they must be that user." This is the bug.
    if authorize(claimed_username, action):
        return f"200 OK — {claimed_username} did {action} (no proof required!)"
    return "403 Forbidden"

print(broken_endpoint("ada", "publish"))  # 'ada' published — and nobody proved they were Ada
```

**Now break it on purpose.** Change the last line to `broken_endpoint("ada", "publish")` from any "attacker" who merely *knows the username* "ada". The broken endpoint never asked for a password — it treated *knowing who Ada is* as *being* Ada. That is the entire Honan attack in four lines.

## 6. Trade-offs: how hard should authentication be?

Authentication strength is a dial, not a switch, and turning it up costs user friction.

| Approach | Evidence | Strength | Cost to the user |
|---|---|---|---|
| Password only | one *know* factor | Weak — passwords leak, get reused, get guessed | Low |
| Password + SMS code | a *know* + a weak *have* | Better, but SMS is interceptable (SIM-swap) | Medium |
| Password + authenticator app / passkey | a *know* + a strong *have* | Strong — the second factor never leaves the device | Medium |
| Passkey only (WebAuthn) | a strong *have*/*are* | Strong and phishing-resistant | Low once set up |

The rule of thumb: **require evidence proportional to what's at stake.** Reading a public chapter needs no authentication at all. Editing and running code on someone else's servers — which costs money and could be abused — needs *some*. Moving money needs *strong, multi-factor* proof. Cortex sits at the low-stakes end and, wisely, **delegates the whole hard problem to GitHub** rather than storing passwords itself — the subject of the next chapter.

## 7. Edge Cases & Failure Modes

- **Treating a fact as a proof.** Knowing your email, your billing address, or your user id is *not* proof you are you. Every "security question" ("mother's maiden name?") is this mistake wearing a hat.
- **Authn without authz.** "The token is valid, so let them through" — but valid *for what*? A token proving you are user A must not grant you access to user B's data. (This is OWASP's #1 risk: Broken Access Control.)
- **Authz without authn.** Granting access based on an unverified claim in the request — a `user_id=42` in the URL that nobody checked you own.
- **Confusing identity with a single identifier.** Usernames get recycled; emails change hands. A *stable, opaque* subject id (we'll see Keycloak's `sub` claim) is the durable identity; the username is just a label.

How would you *notice* you've made one of these mistakes? Ask, for every protected action: *"What, exactly, proved the principal is who they claim — and is that proof fresh, unforgeable, and actually checked here?"* If the answer is "well, they knew the id," you have a Honan.

## 8. Practice

> **Exercise 1 — Sort the questions.** For each, label it authN or authZ: (a) "Is this fingerprint really Sam's?" (b) "Can Sam delete this file?" (c) "Has this token expired?" (d) "Is Sam an administrator?"

<details>
<summary><strong>Hint for Exercise 1</strong></summary>

(a) authN — verifying a credential against a claimed identity. (b) authZ — a permission decision about an action. (c) authN — token validity is part of accepting the identity claim. (d) authZ — "is an administrator" is a permission/role question, asked *after* we know it's Sam.

</details>

<details>
<summary><strong>Answer</strong></summary>

Use the §3 rule: authN *establishes* who the principal is; authZ *uses* that identity to decide on an action. The tell is whether the question interrogates a **credential/identity claim** or a **permission on a resource**.

- **(a) "Is this fingerprint really Sam's?" — authN.** A fingerprint is a credential (an *are*-factor); matching it to the claimed identity "Sam" is verifying a proof of identity.
- **(b) "Can Sam delete this file?" — authZ.** We already know it's Sam; this is a permission decision about a specific action on a specific resource.
- **(c) "Has this token expired?" — authN.** Token freshness is part of accepting (or rejecting) the identity claim the token carries — a stale credential proves nothing, so we can't trust *who* it says you are.
- **(d) "Is Sam an administrator?" — authZ.** "Administrator" is a role/permission question, asked *after* identity is settled, to decide what Sam may do.

Notice (a) and (c) both gate on *identity*, while (b) and (d) presuppose identity and ask about *capability* — that boundary is exactly the authN/authZ seam.

</details>

> **Exercise 2 — Find the seam.** Re-read the Honan story. Identify the *single* policy change at Amazon **or** Apple that would have broken the chain. Was the failure in authentication or in authorization?

<details>
<summary><strong>Answer</strong></summary>

The whole chain was an **authentication** failure: at each step a *fact about* the victim was mistaken for *proof he was* the victim. Several single changes break it; the cleanest is at **Apple** — stop accepting "billing address + last four digits of a card" as identity verification for an Apple ID reset, and instead require a real credential (a one-time code to a verified device, a security question the attacker couldn't look up, a genuine second factor). With that one rule, the last-four digits the attacker harvested from Amazon are worthless and the master key never turns.

A single **Amazon** change works too: refuse to verify a caller using a card number that was *added in the same session* (or refuse to surface last-four of other cards over the phone). Either way the failure to name is authentication, not authorization — no permission check was bypassed; the system simply **accepted weak evidence as a proof of identity**, which is the seam this whole chapter is about.

</details>

> **Exercise 3 — Design the dial.** You're building a school grades portal. Write one sentence each on the authentication strength you'd require for: a student viewing their own grades; a teacher entering grades; an admin exporting the whole school's records. Justify each by what's at stake.

<details>
<summary><strong>Answer</strong></summary>

The §6 rule is *require evidence proportional to what's at stake* — so the dial turns up as the blast radius of a stolen credential grows.

- **Student viewing their own grades — a single strong factor (password or passkey).** The stake is one student's private data; a leak is bad but bounded to that one account, so one good factor is proportionate (a passkey is ideal: strong *and* low-friction).
- **Teacher entering grades — password/passkey + a second factor (MFA).** A teacher can *write* records for many students, so a stolen teacher credential corrupts many people's grades; the extra friction of a second *have*-factor is worth it because the damage is no longer confined to one person.
- **Admin exporting the whole school's records — strong MFA (phishing-resistant passkey/security key), and ideally a fresh re-authentication for the export action.** This account can exfiltrate *everyone's* data in one click, so it gets the strongest, phishing-resistant proof and a step-up challenge at the dangerous action — the cost-of-friction is trivial next to a whole-school breach.

The throughline: friction should scale with the *consequence of impersonation*, not be uniform across all users.

</details>

```quiz
{
  "prompt": "A web app reads `?account_id=1007` from the URL and shows that account's balance, never checking whether the logged-in user owns account 1007. Which failure is this?",
  "input": "Choose one:",
  "options": [
    "Authorization without authentication of the resource owner (broken access control)",
    "A weak authentication factor",
    "An expired credential",
    "A forged token signature"
  ],
  "answer": "Authorization without authentication of the resource owner (broken access control)"
}
```

## In the Wild

- **[Wired — "How Apple and Amazon Security Flaws Led to My Epic Hacking"](https://www.wired.com/2012/08/apple-amazon-mat-honan-hacking/)** (Mat Honan, 2012) — the primary source for the story above. Read it; it is fifteen minutes that will permanently change how you think about "identity verification."
- **[OWASP Top 10 — A01:2021 Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)** — authorization failures are, empirically, the *most common* serious web vulnerability. This is the catalogue.
- **[NIST SP 800-63 — Digital Identity Guidelines](https://pages.nist.gov/800-63-3/)** — the rigorous, government-grade definitions of authentication, factors, and assurance levels. Dense, but the source of truth.

---

**Next:** if storing passwords is so dangerous, what do we do instead? The answer reshaped the entire web — and it begins with a question: *why should you ever give one app your password to another?* → [2. The password anti-pattern](/cortex/production-engineering/iam-keycloak-oauth/the-problem-of-identity/the-password-anti-pattern)
