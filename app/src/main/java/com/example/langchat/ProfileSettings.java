package com.example.langchat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.langchat.API.AuthManager;
import com.example.langchat.API.RetrofitClient;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettings extends AppCompatActivity {

    final int RESULT_LOAD_IMAGE = 10034;

    private Spinner spnLanguage;

    private AuthManager authManager;

    private ArrayList<String> availableLanguages;
    private ArrayAdapter<String> languageAdapter;


    private Button btnLogout;
    private ImageButton btnGoBack;
    private ImageView btnAvatar;

    private TextView tvUsername;


    private String selectedLanguage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authManager = new AuthManager(this);

        if (authManager.getToken() == null || !authManager.isTokenValid()) {
            startActivity(new Intent(this, LoginActivity.class));
            System.out.println("Invalid token, logging out");
            authManager.logout();
            finish();
            return;
        }

        tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(authManager.getJwtProperty("username"));

        spnLanguage = findViewById(R.id.spnLanguage);
        availableLanguages = new ArrayList<>();

        languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableLanguages);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLanguage.setAdapter(languageAdapter);


        // Set the listener to detect item selection
        spnLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();

                if (selectedLanguage.isEmpty()) {
                    selectedLanguage = selectedItem;
                    return;
                }

                if (selectedItem.equals(selectedLanguage)) {
                    return;
                }

                saveGlobalLanguage(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do something when nothing is selected, if needed
            }
        });

        getLanguages();


        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(view -> {
            authManager.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(view -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnAvatar = findViewById(R.id.imgAvatar);
        btnAvatar.setOnClickListener(view -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, RESULT_LOAD_IMAGE);
        });
    }

    private void getUsersDefaultLanguage() {
        Call<String> call = RetrofitClient.getInstance()
                .getAPI().getDefaultPreferredLanguage(authManager.getToken());

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from getUsersDefaultLanguage");
                    return;
                }
                System.out.println(response.body());

                String language = response.body();
                spnLanguage.setSelection(availableLanguages.indexOf(language), false);
                selectedLanguage = language;

            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {

            }
        });
    }


    private void getLanguages() {
        Call<List<String>> call = RetrofitClient.getInstance()
                .getAPI().getAvailableLanguages(authManager.getToken());

        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from getAvailableLanguages");
                    return;
                }
                System.out.println(response.body());
                availableLanguages.addAll(response.body());
                languageAdapter.notifyDataSetChanged();

                getUsersDefaultLanguage();
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {

            }
        });
    }


    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK && reqCode == RESULT_LOAD_IMAGE) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
//                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);


                UCrop.Options options = new UCrop.Options();
                options.setCircleDimmedLayer(true);  // Enable circle crop

                UCrop.of(imageUri, Uri.fromFile(new File(getCacheDir(), "avatar_cropped.jpg")))
                        .withOptions(options)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(100, 100)
                        .start(this);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(ProfileSettings.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        } else if (resultCode == RESULT_OK && reqCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            final InputStream imageStream;
            try {
                imageStream = getContentResolver().openInputStream(resultUri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            // Convert Bitmap to Drawable
            Drawable drawable = new BitmapDrawable(getResources(), selectedImage);

            // Set the drawable as the background of the ImageButton
//            btnAvatar.setBackground(drawable);
            btnAvatar.setImageDrawable(drawable);

        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        } else {
            Toast.makeText(ProfileSettings.this, "You haven't picked Image", Toast.LENGTH_LONG).show();
        }
    }

    private void saveGlobalLanguage(String language) {
        Call<Boolean> call = RetrofitClient.getInstance()
                .getAPI().saveDefaultPreferredLanguage(authManager.getToken(), language);

        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from saveGlobalLanguage" + response.toString());
                    return;
                }
                System.out.println("Success save lang?: " + response.body());

                if (response.body() == true) {
                    Toast.makeText(ProfileSettings.this, "Successfully saved language to: " + language, Toast.LENGTH_SHORT).show();
                    selectedLanguage = language;
                }

            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable throwable) {
                Toast.makeText(ProfileSettings.this, "Unable to save language, please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}