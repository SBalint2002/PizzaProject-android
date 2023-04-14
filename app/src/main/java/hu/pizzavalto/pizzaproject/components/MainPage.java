package hu.pizzavalto.pizzaproject.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import hu.pizzavalto.pizzaproject.R;
import hu.pizzavalto.pizzaproject.auth.JwtResponse;
import hu.pizzavalto.pizzaproject.model.User;
import hu.pizzavalto.pizzaproject.retrofit.NetworkService;
import hu.pizzavalto.pizzaproject.retrofit.ApiService;
import hu.pizzavalto.pizzaproject.sharedpreferences.TokenUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainPage extends AppCompatActivity {
    private TextView profileNameTextView;
    private TextView profileRoleTextView;
    private TextView textTitle;
    private User user;
    private DrawerLayout mainPageLayout;
    private MenuItem logoutMenuItem;
    private NavController navController;
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        init();
        getUserInformation();

        logoutMenuItem.setOnMenuItemClickListener(menuItem -> {
            showLogoutConfirmationDialog();
            return true;
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> textTitle.setText(destination.getLabel()));
    }

    public User getUser(){
        return user;
    }

    private void init(){
        //NavigationView settings
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setItemIconTintList(null);

        //NavigationView header name and role
        View headerView = navigationView.getHeaderView(0);
        profileNameTextView = headerView.findViewById(R.id.ProfileName);
        profileRoleTextView = headerView.findViewById(R.id.ProfileRole);


        Menu menu = navigationView.getMenu();
        logoutMenuItem = menu.findItem(R.id.menuLogout);

        textTitle = findViewById(R.id.textTitle);

        //Display menu from the side
        mainPageLayout = findViewById(R.id.mainPageLayout);
        findViewById(R.id.imageMenu).setOnClickListener(view -> mainPageLayout.openDrawer(GravityCompat.START));


        navController = Navigation.findNavController(this, R.id.navHostFragment);
        NavigationUI.setupWithNavController(navigationView, navController);


    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Biztosan ki akarsz lépni?")
                .setPositiveButton("Igen", (dialogInterface, i) -> {
                    TokenUtils tokenUtils = new TokenUtils(MainPage.this);
                    tokenUtils.clearTokens();
                    navigateToLoginActivity();
                })
                .setNegativeButton("Nem", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void getUserInformation() {
        TokenUtils tokenUtils = new TokenUtils(MainPage.this);
        String accessToken = tokenUtils.getAccessToken();
        if (accessToken == null) {
            navigateToLoginActivity();
            return;
        }

        ApiService apiService = new NetworkService().getRetrofit().create(ApiService.class);
        apiService.getUserInformation("Bearer " + accessToken).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    if (user != null) {
                        profileNameTextView.setText(user.getFirst_name());
                        profileRoleTextView.setText(user.getRole().equals("ADMIN") ? "Adminisztrátor" : "Felhasználó");
                        profileRoleTextView.setTextColor(Color.parseColor(user.getRole().equals("ADMIN") ? "#FF0000" : "#00FF00"));
                        MainPage.this.user = user;
                    } else {
                        navigateToLoginActivity();
                    }

                } else {
                    handleResponseCode(response.code(), tokenUtils, apiService);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                navigateToLoginActivity();
            }
        });
    }

    private void handleResponseCode(int code, TokenUtils tokenUtils, ApiService apiService) {
        if (code == 451) {
            handleTokenRefresh(tokenUtils, apiService);
        } else {
            navigateToLoginActivity();
        }
    }

    private void handleTokenRefresh(TokenUtils tokenUtils, ApiService apiService) {
        String refreshToken = tokenUtils.getRefreshToken();
        if (refreshToken == null) {
            System.out.println("Hiányzó refreshtoken");
            navigateToLoginActivity();
            return;
        }

        TokenUtils.refreshUserToken(tokenUtils, apiService, new Callback<JwtResponse>() {
            @Override
            public void onResponse(@NonNull Call<JwtResponse> call, @NonNull Response<JwtResponse> response) {
                if (response.isSuccessful()) {
                    JwtResponse jwtResponse = response.body();
                    if (jwtResponse != null && jwtResponse.getAccessToken() != null) {
                        tokenUtils.saveAccessToken(jwtResponse.getAccessToken());
                        tokenUtils.setRefreshToken(jwtResponse.getRefreshToken());
                        getUserInformation();
                    } else {
                        navigateToLoginActivity();
                    }
                } else {
                    navigateToLoginActivity();
                }

            }

            @Override
            public void onFailure(@NonNull Call<JwtResponse> call, @NonNull Throwable t) {
                navigateToLoginActivity();
            }
        });
    }

    private void navigateToLoginActivity() {
        startActivity(new Intent(MainPage.this, LoginActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}