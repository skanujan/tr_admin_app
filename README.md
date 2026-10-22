# TR Elegant Boutique - Admin App

A native Android app for managing orders, products, and a daily dashboard,
talking to a new, separate API on trelegantboutique.com. This does not touch
or replace the existing admin website, it is an additional way in.

You do not need to know how to code or use Android Studio to get this app
built and onto your phone, or onto the Play Store. This file is the complete
walkthrough, start to finish. Read it in order the first time.

## What you already have

- The app's source code (this project).
- A release signing key already generated for you (see "Your signing key"
  below). This is the one and only key this app can ever be signed with.
  Losing it means you could never update the app on the Play Store again
  under the same listing, so the three files below need a safe backup
  (a password manager, or a USB drive kept somewhere safe) in addition to
  being added to GitHub as described below.

## Part 1: Put this project on GitHub

Building the app requires tools (the Android build system) that only run in
GitHub's own cloud, not on an ordinary computer. So the process is: upload
this project to a GitHub repository, and a prewritten automation
(`.github/workflows/build.yml`, already included) builds the app there every
time you update the project.

1. Go to github.com and create a free account if you do not already have
   one.
2. Click the "+" in the top right, then "New repository". Name it something
   like `tr-admin-app`. Choose "Private" (this is your business's app, it
   should not be public). Do not check any of the boxes for adding a README
   or .gitignore, since this project already has its own. Click
   "Create repository".
3. On the page that follows, look for "uploading an existing file" (a link
   in the quick setup instructions). Click it.
4. Open this `tr-admin-app` folder specifically (not its parent folder) and
   drag everything inside it into the browser window: all the files and
   folders at once, including the hidden `.github` folder (if your file
   manager hides it, turn on "show hidden files" first). Do **not** include
   the separate `signing-key-KEEP-SAFE` folder that sits next to this one,
   that one never goes to GitHub (see "Your signing key" near the end of
   this file). Wait for the upload to finish, then click "Commit changes".

Your project is now on GitHub. The automation has not run yet, it runs once
you add the signing key as described next.

## Part 2: Add your signing key as a secret

"Secrets" are where GitHub stores private values (like passwords) so the
automation can use them without them ever appearing in your code.

1. In your new repository on GitHub, click "Settings" (top menu of the repo,
   not your account settings), then in the left sidebar: "Secrets and
   variables" > "Actions".
2. Click "New repository secret" and add these four, one at a time (name on
   the left, value on the right, exactly as shown):

   | Secret name | Value |
   |---|---|
   | `KEYSTORE_BASE64` | the contents of the file `tr-boutique-admin-release.keystore.b64` |
   | `KEYSTORE_PASSWORD` | the password from `credentials.txt` |
   | `KEY_ALIAS` | `tr-boutique-admin` |
   | `KEY_PASSWORD` | the same password from `credentials.txt` (it is reused for both) |

3. Click "Actions" in the repo's top menu. You should see a build already
   running (or about to run) from your upload in Part 1. Click into it to
   watch its progress. It usually finishes in a few minutes.
4. Once it finishes with a green checkmark, scroll down to "Artifacts" at
   the bottom of that run's page. You will see two downloads:
   - `app-release-aab` - this is what you upload to the Play Store.
   - `app-release-apk` - this is a file you can install directly on an
     Android phone for testing, without going through the Play Store (see
     "Testing before you publish" below).

If the build fails (a red X instead of a green check), click into it, open
the failed step, and you will see the error near the bottom of the log. The
most common cause is a secret typed slightly wrong, double-check the four
values above.

## Testing before you publish

Download `app-release-apk` from the Actions run, unzip it (GitHub wraps
artifacts in a .zip), and copy the `.apk` file onto an Android phone (for
example by emailing it to yourself, or using a USB cable). Open it on the
phone from the Files app. Android will warn about installing from outside
the Play Store, this is normal for an app you built yourself, allow it.

## Part 3: Create your Google Play Developer account

This is the account that lets you publish apps to the Play Store. It is
separate from, and cannot be set up by, anyone but you, since it is tied to
your own identity and a one-time payment.

1. Go to play.google.com/console and sign in with the Google account you
   want to manage the Boutique's apps with.
2. Pay the one-time $25 registration fee.
3. Complete identity verification. Google now requires this for all new
   developer accounts, it can take anywhere from a few hours to a few days
   and may ask for a government ID and other business details.
4. Once approved, click "Create app" inside Play Console. Fill in:
   - App name: "TR Elegant Boutique Admin" (or your preferred name)
   - Default language
   - App or game: App
   - Free or paid: Free
   - Declarations: confirm it meets Play's policies (it is an internal
     admin tool, not consumer-facing, which you can note in the app's
     description)

## Part 4: Fill in the store listing and upload the app

Play Console will walk you through a checklist for your new app. The parts
that need your input:

- **Privacy policy**: Google requires a privacy policy URL for every app,
  even an internal one, since it reads account data. If your main website
  does not have one yet, a short page describing what the app accesses
  (order and product data, for your own staff logins only) and linking it
  from trelegantboutique.com will satisfy this.
- **App content / content rating questionnaire**: a short questionnaire
  about the app's content. For an internal admin tool, nearly everything
  will be "no."
- **Store listing**: a short description, a full description, and
  screenshots (at least 2) of the app running. Take these from your phone
  after installing the test APK above.
- **App icon**: the app currently ships with a placeholder gold "TR"
  monogram icon. You can replace
  `app/src/main/res/drawable/ic_launcher_foreground.xml` with your own
  design at any time (ask for help with this when you are ready) and the
  build will pick it up automatically on the next run.
- **Target audience**: since this is a staff-only tool, choose an adult
  audience.

Once the checklist is complete, go to "Production" (left sidebar, under
"Release"), click "Create new release", and upload the `app-release.aab`
file you downloaded from GitHub Actions. Submit for review.

Google's review for a first-time app typically takes a few days to about a
week. After that, the app is live on the Play Store, and every future
update follows the same loop: change the code (or have it changed for you),
push the update to GitHub, download the new `.aab` from the Actions run,
upload it as a new release in Play Console.

## Signing in to the app itself

The app logs in with the same email and password as any admin account on
the website (Settings > Admins on the website, or ask whoever manages
admin accounts there). The first time you sign in on a phone, if that phone
has a fingerprint sensor set up, the app turns on fingerprint unlock for
all future opens, so the password is only needed again if you sign out.

## What this app can do (version 1)

- **Dashboard**: today's orders and revenue, orders by status, low-stock
  products.
- **Orders**: browse and search all orders, filter by status, open one to
  see its items and change its status (the same effects as the website:
  stock is restocked automatically on a cancellation or return, customers
  get the same WhatsApp notifications on shipping/delivery).
- **Products**: browse and search products, open one to toggle it
  active/inactive, and adjust its stock and price.

Other parts of the admin website (WhatsApp template/campaign tools,
banners, Domex settlement, reports, admin roles) are not in this first
version. They can be added later the same way these were, by extending the
API and adding a matching screen.

## Your signing key

Three files were generated for this app and are in a separate folder next
to this one, called `signing-key-KEEP-SAFE` (NOT inside this `tr-admin-app`
folder, on purpose):

- `tr-boutique-admin-release.keystore` - the actual signing key.
- `tr-boutique-admin-release.keystore.b64` - the same key, encoded as text,
  for pasting into the `KEYSTORE_BASE64` GitHub secret above.
- `credentials.txt` - the password.

**Do not upload the `signing-key-KEEP-SAFE` folder to GitHub.** Only this
`tr-admin-app` folder goes into the repository in Part 1. The signing key
only ever needs to go into the GitHub secret values in Part 2, never into
the repository's files.

Keep a backup of all three somewhere safe outside of GitHub (for example, a
password manager or an encrypted USB drive). If they are lost, this specific
app listing could never be updated again, a new one would have to be
created from scratch and the old one retired.
