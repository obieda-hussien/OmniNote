# Privacy policy hosting

Enable **Settings → Pages → Build and deployment → Source → GitHub Actions**
once, then run **Publish privacy policy** from Actions (or update the policy on
`main`). Use the verified Pages deployment URL in Google Play Console.

The workflow publishes only `index.html` and `privacy_policy.html`, generated
from the root privacy-policy file. It never publishes the repository root,
Android sources, Git history, APKs, AABs, keystores, or CI build artifacts.
The policy itself is intentionally public and can be downloaded by visitors.

GitHub Pages from a private personal repository requires **GitHub Pro**.
With that plan, keep the Pages site public while making the application
repository private. With GitHub Free, use a separate public repository holding
only the privacy page; do not make the application source public for hosting.

Repository privacy blocks unauthenticated source/raw/archive access. Users and
apps granted repository access can still read or clone it. Copies previously
downloaded while the repository was public cannot be revoked.

Do not change the Play Console privacy URL until the new deployment works
without signing in. The existing policy host remains available during migration.
