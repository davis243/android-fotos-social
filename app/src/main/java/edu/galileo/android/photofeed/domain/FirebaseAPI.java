package edu.galileo.android.photofeed.domain;



import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import edu.galileo.android.photofeed.entities.Photo;
import edu.galileo.android.photofeed.lib.GreenRobotEventBus;
import edu.galileo.android.photofeed.lib.base.EventBus;
import edu.galileo.android.photofeed.login.events.LoginEvent;

/**
 * Created by ykro.
 */
public class FirebaseAPI {
    private DatabaseReference firebase;
    private ChildEventListener photosEventListener;
    DatabaseReference myUserReference ;
    private final static String USERS_PATH = "users";


    public FirebaseAPI(DatabaseReference firebase) {
        this.firebase = firebase;
    }

    public void checkForData(final FirebaseActionListenerCallback listener){
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 0) {
                    listener.onSuccess();
                } else {
                    listener.onError(null);
                }
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                listener.onError(firebaseError);
            }
        });
    }

    public void subscribe(final FirebaseEventListenerCallback listener) {
        if (photosEventListener == null) {
            photosEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    listener.onChildAdded(dataSnapshot);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    listener.onChildRemoved(dataSnapshot);
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError firebaseError) {
                    listener.onCancelled(firebaseError);
                }
            };
            firebase.addChildEventListener(photosEventListener);
        }
    }

    public void unsubscribe() {
        firebase.removeEventListener(photosEventListener);
    }

    public String create() {
        return firebase.push().getKey();
    }

    public void update(Photo photo) {
        DatabaseReference reference = this.firebase.child(photo.getId());
        reference.setValue(photo);
    }

    public void remove(Photo photo, FirebaseActionListenerCallback listener) {
        firebase.child(photo.getId()).removeValue();
        listener.onSuccess();
    }

    public String getAuthEmail(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = null;
        if (user != null) {
            email = user.getEmail();
        }
        return email;
    }

    public void signUp(final String email, final String password, final FirebaseActionListenerCallback listener){
     /*   firebase.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                listener.onSuccess();
            }

            @Override
            public void onError(DatabaseError firebaseError) {
                listener.onError(firebaseError);
            }
        });*/
        try {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        postEvent(LoginEvent.onSignUpSuccess);
                        login(email, password);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        postEvent(LoginEvent.onSignUpError, e.getMessage());
                    }
                });
        } catch (Exception e) {
            postEvent(LoginEvent.onSignUpError, e.getMessage());
        }
    }

    public void login(String email, String password/*, final FirebaseActionListenerCallback listener*/){
        /*firebase.authWithPassword(email, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                listener.onSuccess();
            }

            @Override
            public void onAuthenticationError(DatabaseError firebaseError) {
                listener.onError(firebaseError);
            }
        });*/
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            myUserReference = getMyUserReference();
                            myUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    initSignIn(snapshot);
                                }
                                @Override
                                public void onCancelled(DatabaseError firebaseError) {
                                    postEvent(LoginEvent.onSignInError, firebaseError.getMessage());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            postEvent(LoginEvent.onSignInError, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            postEvent(LoginEvent.onSignInError, e.getMessage());
        }

    }
    private void initSignIn(DataSnapshot snapshot){
        User currentUser = snapshot.getValue(User.class);

        if (currentUser == null) {
            registerNewUser();
        }
        changeUserConnectionStatus(User.ONLINE);
        postEvent(LoginEvent.onSignInSuccess);
    }
    private void registerNewUser() {
        String email = getAuthUserEmail();
        if (email != null) {
            User currentUser = new User(email, true, null);
            myUserReference.setValue(currentUser);
        }
    }
    public void changeUserConnectionStatus(boolean online) {
        if (getMyUserReference() != null) {
            Map<String, Object> updates = new HashMap<String, Object>();
            updates.put("online", online);
            getMyUserReference().updateChildren(updates);


        }
    }

    public DatabaseReference getMyUserReference() {
        return getUserReference(getAuthUserEmail());
    }
    public DatabaseReference getUserReference(String email){
        DatabaseReference userReference = null;
        if (email != null) {
            String emailKey = email.replace(".", "_");
            userReference = firebase.getRoot().child(USERS_PATH).child(emailKey);
        }
        return userReference;
    }
    public String getAuthUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = null;
        if (user != null) {
            email = user.getEmail();
        }
        return email;
    }
    private void postEvent(int type) {
        postEvent(type, null);
    }

    private void postEvent(int type, String errorMessage) {
        LoginEvent loginEvent = new LoginEvent();
        loginEvent.setEventType(type);
        if (errorMessage != null) {
            loginEvent.setErrorMesage(errorMessage);
        }

        EventBus eventBus = GreenRobotEventBus.getInstance();
        eventBus.post(loginEvent);
    }
    public void checkForSession(FirebaseActionListenerCallback listener) {
      /*  if (firebase.getAuth() != null) {
            listener.onSuccess();
        } else {
            listener.onError(null);
        }*/
        FirebaseAuth auth = FirebaseAuth.getInstance();
        final FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            listener.onSuccess();
        } else {
            listener.onError(null);
        }

    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
    }
}
