This Java application helps users manage and moderate online Mafia games. It connects to a forum thread and parses the HTML, keeping track of custom mafia commands and generating information such as vote counts and post counts. 

The primary goal of this tool is to assist Game Moderators in managing and keeping track of their games; it is not a substitute for actual policing! Game Moderators should still be actively monitoring their game thread.

If you are a Game Moderator and would like to use this tool, feel free to message me with a link to your mafia thread and I’ll send you a download link.

# Features:#

* No set-up required! Simply run the MafiaBot.jar file and the parser will do the rest!

* Vote counts! (running tallies on votes made in the current phase)

* Post counts! (tracks how many posts each player makes per day)

* Nicknames & fuzzy name matching for votes!

* Finds edited posts and provides a link to them

* Displays an alert for player actions and provides a link to them

* Thread caching! Parsed posts will be downloaded and saved to a file in the same directory as MafiaBot.jar; this makes subsequent runs much faster.

# Usage (basic): #
Simply ensure your players use bolded votes like so: **##vote Alex**

Players can remove their vote with a bolded **##unvote**

Unvoting is optional. Players do not need to unvote before voting for someone else.

The application will count all the **bolded** votes and display a tally.

# Usage (advanced): #

The following commands are optional but are provided to you for greater control over the game.

* **##kill <player>** - Kills the player. Dead players are not allowed to vote and cannot be voted upon. Active votes made by or made on the dead player are removed.

* **##resurrect <player>** - Resurrects the player so they can vote and be voted on again.

* **##setvoteweight <player> <num>** - Changes the weight of a player’s vote. By default, every player has a vote weight of 1.

* **##setvotenum <player> <num>** - Changes the number of votes a player has. By default, every player has one vote. To use their multiple votes, players simply ##vote multiple times; and ##unvote will remove all their votes. Players cannot stack multiple votes on one person.

* **##takevote <player>** - Removes the player’s ability to vote until you give it back.

* **##givevote <player>** - Reinstates the player’s ability to vote

* **##strikevote <player>** - Forces the player to unvote

* **##pardon <player>** - Removes all votes on a player

* **##purgevotes** - Removes everyone’s votes

* **##addplayer <player>** / ##removeplayer <player> - Adds / removes a player from the game. 

* **##addnpc <name> / ##removenpc <name>** - Adds / removes an npc from the game. Npcs cannot vote, but can be voted upon. Useful if you want players to vote on something that’s not another player. A default NPC is "No lynch".

* **##gm <name>** – Gives someone GM permissions so they can use the above GM commands. The thread maker is automatically granted GM permissions.



***
# Example vote count: #

```
#!html

Current vote count (Night 14):
(4/4) No vote: [Category 5, Hylius, Lorcan, oneyou]

4 players alive: 
Category 5
Hylius
Lorcan
oneyou

18 players dead:
Aatxe360
BaneOfSorrows
Blakmage86
Dashermkii
Gidonihah
Hawkwings
Jijonbreaker
Kamil
Lorce
Mattiator
Retaliation
Sandman0893
Sporkosophy
Tanki
theGECK
Tyzh
Window
Zylo

```

***
# Example post count: #

```
#!html

Post count:
                [ D1] [ D2] [ D3] [ D4] [ D5] [ D6] [ D7] [ D8] [ D9] [D10] [D11] [D12] [D13] [D14] [Total]
Tanki:          [  8] [ 12] [ 30] [ 40] [ 61] [ 54] [ 48] [ 18] [ 19] [  1] [  0] [  0] [  0] [  9] [ 300] 
Category 5:     [  0] [  0] [  4] [ 11] [ 37] [ 43] [ 57] [ 40] [  6] [ 21] [  5] [ 23] [ 12] [ 40] [ 299] 
Hylius:         [ 14] [ 22] [ 21] [ 30] [ 17] [  6] [ 48] [ 27] [  0] [ 11] [ 19] [ 10] [ 16] [ 27] [ 268] 
Tyzh:           [ 51] [ 57] [ 42] [  0] [ 18] [ 12] [ 19] [ 46] [  1] [  0] [  0] [  0] [  0] [  6] [ 252] 
Jijonbreaker:   [ 30] [ 14] [ 36] [ 12] [ 55] [ 20] [ 17] [  9] [  7] [ 10] [ 11] [  8] [  7] [  5] [ 241] 
oneyou:         [ 10] [ 12] [ 25] [ 36] [ 24] [ 15] [  9] [ 18] [ 11] [ 17] [ 11] [ 16] [  4] [ 19] [ 227] 
Sporkosophy:    [ 31] [ 25] [ 45] [ 31] [  0] [  4] [  2] [  4] [  1] [  3] [ 17] [  8] [  0] [  1] [ 172] 
Lorcan:         [ 24] [ 18] [ 15] [ 24] [  0] [  4] [  1] [  9] [  6] [  6] [  7] [  4] [  2] [  6] [ 126] 
BaneOfSorrows:  [  8] [ 15] [ 24] [ 21] [ 18] [ 25] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  1] [ 112] 
Zylo:           [ 19] [ 29] [ 43] [ 20] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [ 111] 
Retaliation:    [  0] [  0] [  9] [ 17] [  7] [  2] [ 16] [  7] [  5] [ 11] [  6] [  8] [  4] [  7] [  99] 
Kamil:          [  6] [  6] [  8] [  6] [  7] [  6] [ 10] [  5] [  6] [  1] [  4] [  7] [  0] [  1] [  73] 
Blakmage86:     [ 23] [ 11] [  5] [ 12] [  2] [ 18] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  1] [  72] 
Aatxe360:       [ 34] [ 27] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  1] [  62] 
Dashermkii:     [  4] [  8] [  8] [ 13] [  0] [  3] [  8] [ 10] [  1] [  3] [  0] [  0] [  0] [  0] [  58] 
Window:         [ 18] [ 39] [  1] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  58] 
Hawkwings:      [  8] [  6] [  0] [  8] [  2] [ 15] [ 14] [  0] [  0] [  0] [  0] [  0] [  0] [  1] [  54] 
Lorce:          [ 12] [ 15] [ 13] [  3] [  0] [  8] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  1] [  52] 
Sandman0893:    [ 16] [  7] [  8] [ 12] [  1] [  2] [  1] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  47] 
theGECK:        [  0] [  0] [  0] [  0] [  2] [  4] [  8] [  2] [  3] [  2] [  2] [  7] [  3] [  3] [  36] 
Gidonihah:      [  8] [  0] [  1] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  6] [  15] 
Mattiator:      [  6] [  2] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [  0] [   8] 

```