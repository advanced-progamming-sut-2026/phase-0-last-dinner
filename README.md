group name : last supper
members:
1.matin khorasani : 404106088
2.sepanta saeidi : 404105915
3.shayan pourghafar : 404105615

## Phase one account commands

The application starts in the signup menu unless a user selected stay logged in.
User accounts are stored outside the project in `.plants-vs-zombies-2/users.json`
under the operating-system user directory.

```text
menu show current
menu enter <menu_name>
menu exit

register -u <username> -p <password> <password_confirm> -n <nickname> -e <email> -g <gender>
pick question -q <question_number> -a <answer> -c <answer_confirm>

login -u <username> -p <password> -stay-logged-in
forget password -u <username> -e <email>
answer -a <answer>
new password -p <password> <password_confirm>

menu logout
```
