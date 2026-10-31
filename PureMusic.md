Project Goal

Our goal is to create a restricted version of Metrolist that gives users control over the music content accessible through the app. The app will include a password-protected Control and Management screen, allowing users to hide images and restrict all music access to a user-defined list of approved artists.

These restrictions must be enforced consistently throughout the app, regardless of how the content is accessed or whether it was previously accessed through other clients.

To-Do List

Control and Management Screen

Create a dedicated Control and Management screen.

Password Setup and Authentication

The first time the user opens this screen, they must choose a password and confirm it by entering it a second time.

If both passwords match, the password will be saved and required for all future access to this screen.

If they do not match, display a message informing the user that the passwords do not match and ask them to try again.

Image Visibility

Add an option in this screen to enable or disable image display.

If image display is disabled, all images throughout the app must be hidden, including album covers, artist pictures, and other images.

Artist Restrictions

Add an option in this screen to enable or disable artist restrictions.

Selecting Approved Artists

If artist restrictions are enabled, the user must be able to search through all artists available on YouTube Music and select the artists they want to allow.

Enforcing Artist Restrictions Throughout the App

On the home screen, in recommendations, search results, when opening links, and through any other way of accessing music, only songs by artists selected by the user may be displayed or played.

No other music content must be accessible.

If the user attempts to access restricted content directly (for example, through a direct link), display the following toast message:

This content has been blocked by your settings.

Restrictions Across Different Clients

Restrictions must also apply to songs and other music content that the user has accessed through other clients.

Even if the user follows an artist in another client, or that artist is popular in their account, the content must still be blocked if that artist has not been approved in this app.

The approved artist list configured in this app must take precedence over artist preferences, follows, and listening history from other clients.