package fi.aalto.legroup.achso.entities;

import java.util.ArrayList;

/**
 * Use this class to store users memory efficiently.
 * Maps User objects to indices that can later retrieve the actual User back.
 */
public class UserPool {

    protected static ArrayList<User> userPool = new ArrayList<>();

    /**
     * Make the user object unique.
     * @param user User object to make unique.
     * @return Index to the user in the pool.
     */
    public static int internUser(User user) {

        // Map null to -1
        if (user == null) {
            return -1;
        }

        // Try to find the user from the pool.
        int maxUsers = userPool.size();
        for (int i = 0; i < maxUsers; i++) {
            if (userPool.get(i).equals(user))
                return i;
        }

        // If not found add it and return the index to the last element (the added user).
        userPool.add(user);
        return maxUsers;
    }

    /**
     * Retrieve the unique User object.
     * @param index User object to make unique.
     * @return The user object for the index
     */
    public static User getInternedUser(int index) {

        // Map null to -1
        if (index == -1)
            return null;

        return userPool.get(index);
    }

}
