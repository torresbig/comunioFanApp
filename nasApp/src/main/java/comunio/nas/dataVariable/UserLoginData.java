package comunio.nas.dataVariable;

public class UserLoginData {

    private String username;
    private char[] passwort;
    private String gitName;
    private String gitProjectName; 
    private String gitToken; 
    private boolean debug;
    
    public UserLoginData(String[] args) {
		if (args.length >= 5) {
			this.username = args[0];
			setPasswort(args[1]); // wandelt String in char[] um
			this.gitName = args[2];
			this.gitProjectName = args[3];
			this.setGitToken(args[4]);
			if (args.length >= 6) {
				this.setDebug(args[5].equalsIgnoreCase("DEBUG"));
			} else {
				this.setDebug(false);
			}
		} else {
			throw new IllegalArgumentException("Es werden mindestens 5 Argumente benötigt: <username> <passwort> <gitName> <gitProjectName> <gitToken> [<debug>]");
		}
	}

    public UserLoginData(String username, String passwort, String gitName, String gitProjectName, String gitToken, boolean debug) {
        this.username = username;
        setPasswort(passwort); // wandelt String in char[] um
        this.gitName = gitName;
        this.gitProjectName = gitProjectName;
        this.setGitToken(gitToken);
        this.setDebug(debug); 
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return das Passwort als char-Array
     */
    public char[] getPasswort() {
        return passwort;
    }

    /**
     * @return das Passwort als String (z.B. zur Verwendung)
     */
    public String getPasswortAlsString() {
        if (passwort == null) return null;
        return new String(passwort);
    }

    /**
     * @param passwort das Passwort als String; wird intern in char[] umgewandelt
     */
    public void setPasswort(String passwort) {
        if (passwort != null) {
            this.passwort = passwort.toCharArray();
        } else {
            this.passwort = null;
        }
    }

    /**
     * Überschreibt das Passwort im Speicher zur Sicherheit
     */
    public void clearPasswort() {
        if (passwort != null) {
            for (int i = 0; i < passwort.length; i++) {
                passwort[i] = 0;
            }
        }
    }

    /**
     * @return the gitName
     */
    public String getGitName() {
        return gitName;
    }

    /**
     * @param gitName the gitName to set
     */
    public void setGitName(String gitName) {
        this.gitName = gitName;
    }

    /**
     * @return the gitProjectName
     */
    public String getGitProjectName() {
        return gitProjectName;
    }

    /**
     * @param gitProjectName the gitProjectName to set
     */
    public void setGitProjectName(String gitProjectName) {
        this.gitProjectName = gitProjectName;
    }
    
    public String getCompleteGitProjectPath(String filename) {
    	return "https://raw.githubusercontent.com/"+ this.getGitName() + "/" + this.getGitProjectName() +"/main/data/" + filename;
    }

	public String getGitToken() {
		return gitToken;
	}

	public void setGitToken(String gitToken) {
		this.gitToken = gitToken;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public String[] toStringArray() {
		return new String[] {
			this.username,
			this.getPasswortAlsString(),
			this.gitName,
			this.gitProjectName,
			this.gitToken,
			this.debug ? "DEBUG" : "NODEBUG"
		};
	}
	

}
