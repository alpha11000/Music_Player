package com.example.music_player;

import static com.example.music_player.enumsAndGlobals.SortKey.SORT_DATE;
import static com.example.music_player.enumsAndGlobals.SortKey.SORT_NAME;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.music_player.adapters.FragmentAdapter;
import com.example.music_player.enumsAndGlobals.SortKey;
import com.example.music_player.interfacesAndAbstracts.InputEnterCallback;
import com.example.music_player.interfacesAndAbstracts.SongListCallback;
import com.example.music_player.metadata.PlaylistMetadata;
import com.example.music_player.metadata.SongMetadata;
import com.example.music_player.utils.FileUtil;
import com.example.music_player.utils.PlaylistsUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Toolbar toolbar;
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    FragmentAdapter fragmentAdapter;

    SortKey[] actualSortSettings;

    DrawerLayout drawerLayout;
    NavigationView navigationView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //variables attributions
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.main_tab_layout);
        viewPager2 = findViewById(R.id.main_view_pager);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.lateral_navigation_view);

        //importing sort settings from storage
        actualSortSettings = FileUtil.readSortSettings(this);

        //defining toolbar
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle("Music Player");
        }

        //toolbar toggle button
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_lateral_navigation, R.string.close_lateral_navigation);

        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.primary_white));

        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //defining tab layout tabs
        addMultipleTabsToTabLayout(tabLayout, "Tracks", "Playlists", "Artists", "Albums");

        //setting view pager
        fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), getLifecycle(), actualSortSettings);
        viewPager2.setAdapter(fragmentAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        navigationView.setNavigationItemSelectedListener(i -> {
                int pos = i.getOrder();

                if(pos < 3)
                    viewPager2.setCurrentItem(pos);

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        SongPlayer.addSongListListener(new SongListCallback() {
            @Override
            public void onSongChanged(SongMetadata currentPlayingSong) {
                String name = (currentPlayingSong == null) ?
                        "Finished" :
                        currentPlayingSong.getName();

                Toast.makeText(MainActivity.this, name, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showTextInputDialog(@NonNull Context context, @NonNull String title,@NonNull InputEnterCallback inputEnterCallback){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, R.style.InputDialogTheme);

        final EditText editText = new EditText(context);
        editText.setSingleLine();

        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setView(editText);

        alertDialogBuilder.setPositiveButton("Save", (dialogInterface, i) -> {
            inputEnterCallback.onInputEnter(editText.getText().toString());
        });

        alertDialogBuilder.show();
    }

    public static void showPlaylistsDialog(@NonNull Context context, @NonNull InputEnterCallback inputEnterCallback){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, R.style.InputDialogTheme);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.playlist_names_dialog, null);

        ListView listView = view.findViewById(R.id.playlists_listview);
        Button newPlaylistButton = view.findViewById(R.id.new_playlist_button);

        ArrayList<PlaylistMetadata> playlists = PlaylistsUtil.getPlaylists(context);
        ArrayList<String> playlistsNames= new ArrayList<>();

        for(PlaylistMetadata playlist : playlists){
            playlistsNames.add(playlist.getName());
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter(context, R.layout.list_view_item, playlistsNames);
        listView.setAdapter(arrayAdapter);

        alertDialogBuilder.setView(view);
        AlertDialog alertDialog = alertDialogBuilder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                inputEnterCallback.onInputEnter(position);
                alertDialog.cancel();
            }
        });

        newPlaylistButton.setOnClickListener(v -> {
            showTextInputDialog(view.getContext(), "Playlist name", new InputEnterCallback() {
                @Override
                public void onInputEnter(String input) {
                    PlaylistMetadata playlist = new PlaylistMetadata(input, 0);
                    PlaylistsUtil.addPlaylist(playlist, view.getContext());

                    inputEnterCallback.onInputEnter(playlists.size() -1);
                    alertDialog.cancel();
                }

                @Override public void onInputEnter(int position) {}
            });
        });

        alertDialog.show();
    }

    private void addMultipleTabsToTabLayout(@NonNull TabLayout tabLayout, @NonNull String... tabNames){

        for (String tabName: tabNames) {
            TabLayout.Tab tab = tabLayout.newTab().setText(tabName);
            tabLayout.addTab(tab);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_toolbar_menu, menu);

        MenuItem.OnActionExpandListener onActionExpandListener = new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                enterSearchModeOnActualFragment();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                exitSearchModeOnActualFragment();
                return true;
            }
        };

        menu.findItem(R.id.search_button).setOnActionExpandListener(onActionExpandListener);
        SearchView searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
        searchView.setQueryHint("Music name...");
        searchView.setIconifiedByDefault(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                findItemByNameOnActualFragment(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            exitSearchModeOnActualFragment();
            Toast.makeText(this, "CLOSED", Toast.LENGTH_LONG).show();
            return true;
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sort_name_option:
                sortActualFragmentItems(SORT_NAME);
                break;
            case R.id.sort_date_option:
                sortActualFragmentItems(SORT_DATE);
                break;
        }

        return true;
    }

    private void sortActualFragmentItems(SortKey sortKey){
        fragmentAdapter.sortElementsOnFragment(viewPager2.getCurrentItem(), sortKey);
        actualSortSettings[viewPager2.getCurrentItem()] = sortKey;
        FileUtil.saveSortSettings(actualSortSettings, this);
    }

    private void enterSearchModeOnActualFragment(){
        viewPager2.setUserInputEnabled(false);
        tabLayout.setVisibility(View.GONE);
        fragmentAdapter.enterSearchModeOnFragment(viewPager2.getCurrentItem());
    }

    private void findItemByNameOnActualFragment(String name){
        fragmentAdapter.findItemByNameOnFragment(viewPager2.getCurrentItem(), name);
    }

    private void exitSearchModeOnActualFragment(){
        fragmentAdapter.exitSearchModeOnFragment(viewPager2.getCurrentItem());
        tabLayout.setVisibility(View.VISIBLE);
        viewPager2.setUserInputEnabled(true);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public static void showPopupMenu(Context context, View view, @MenuRes int menu, PopupMenu.OnMenuItemClickListener onItemSelected){
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.setOnMenuItemClickListener(onItemSelected);

        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(menu, popupMenu.getMenu());
        popupMenu.show();
    }
}