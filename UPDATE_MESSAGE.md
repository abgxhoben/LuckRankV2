## Update 3.0

This release improves rank safety, reload stability, database concurrency, packaging, and compatibility.

### Changes
- Reworked the project packaging so the final plugin jars are built more cleanly
- Fixed missing shared classes in the packaged plugin output
- Improved compatibility for older Spigot environments
- Adjusted LuckPerms loading behavior so the dependency is handled more reliably
- Improved Discord webhook handling and validation
- Added better webhook error logging for easier troubleshooting
- Cleaned up the project structure for future maintenance

### Notes
- Make sure LuckPerms is installed before starting LuckRank
- If you use Discord webhooks, check that your webhook URL and optional image URLs are valid
- Please replace older test jars with the newly built final plugin jar

Thank you for using LuckRank.
