package edu.galileo.android.photofeed.domain.di;

import android.content.Context;
import android.location.Geocoder;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import edu.galileo.android.photofeed.domain.FirebaseAPI;
import edu.galileo.android.photofeed.domain.Util;

/**
 * Created by ykro.
 */
@Module
public class DomainModule {
    private final static String FIREBASE_URL = "https://photofeed-45e0b.firebaseio.com/";

    @Provides
    @Singleton
    FirebaseAPI providesFirebaseAPI(DatabaseReference firebase) {
        return new FirebaseAPI(firebase);
    }

    @Provides
    @Singleton
    DatabaseReference providesFirebase() { return FirebaseDatabase.getInstance().getReferenceFromUrl(FIREBASE_URL);
    }

    @Provides
    @Singleton
    Util providesUtil(Geocoder geocoder) {
        return new Util(geocoder);
    }

    @Provides
    @Singleton
    Geocoder providesGeocoder(Context context) {
        return new Geocoder(context);
    }
}
