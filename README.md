Simple client server audio streaming project

Note: so far can only play .wav files

_To Compile:_
1. First terminal: javac Server.java
2. Second terminal: javac Client.java

_To Run:_
1. First terminal: java Server <Port Number>
2. Second terminal: java Client <Server IP> <Port Number>
3. Log in with appropriate account
   1. As a User
      1. username: user
      2. password: pass
   2. As an Admin
      1.  username: admin
      2.  password: pass
4.  Enter Commands
    1.  Admin Only
        1. add <song name> *Note: song must be in current directory of Client.java*
        2. delete <song name>
        3. create <username> <pass>
        4. remoev <username>
    2. All Users
       1. play <song name>
       2. pause
       3. resume
       4. stop
       5. library 


Future work is to get .mp3 files to work and also add ability to use playlists