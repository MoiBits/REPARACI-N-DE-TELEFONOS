package com.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepairListActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private RepairOrderAdapter adapter;
    private AppDatabase db;
    private TabLayout tabLayout;
    private SearchView searchView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_list);

        db = AppDatabase.getInstance(this);
        initViews();
        setupTabs();
        setupSearch();
        setupSwipeToDelete();
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rvOrders);
        tabLayout = findViewById(R.id.tabLayout);
        searchView = findViewById(R.id.searchView);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepairOrderAdapter();
        rvOrders.setAdapter(adapter);

        int selectedTab = getIntent().getIntExtra("selected_tab", 0);
        if (tabLayout != null && tabLayout.getTabCount() > selectedTab) {
            TabLayout.Tab tab = tabLayout.getTabAt(selectedTab);
            if (tab != null) {
                tab.select();
            }
        }
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadOrdersByFilter(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadOrdersByFilter(tab.getPosition());
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchOrders(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadOrdersByFilter(tabLayout.getSelectedTabPosition());
                } else {
                    searchOrders(newText);
                }
                return true;
            }
        });
    }

    private void loadOrdersByFilter(int tabPosition) {
        executor.execute(() -> {
            List<RepairOrder> orders;
            switch (tabPosition) {
                case 1:
                    orders = db.repairOrderDao().getOrdersByStatus("Pendiente");
                    break;
                case 2:
                    orders = db.repairOrderDao().getOrdersByStatus("En Taller");
                    break;
                case 3:
                    orders = db.repairOrderDao().getOrdersByStatus("Listo");
                    break;
                case 4:
                    orders = db.repairOrderDao().getOrdersByStatus("Entregado");
                    break;
                case 0:
                default:
                    orders = db.repairOrderDao().getAllOrders();
                    break;
            }
            runOnUiThread(() -> adapter.setOrders(orders));
        });
    }

    private void searchOrders(String query) {
        executor.execute(() -> {
            List<RepairOrder> orders = db.repairOrderDao().searchOrders(query);
            runOnUiThread(() -> adapter.setOrders(orders));
        });
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                RepairOrder orderToDelete = adapter.getOrders().get(position);

                new AlertDialog.Builder(RepairListActivity.this)
                        .setTitle("Eliminar Reparación")
                        .setMessage("¿Estás seguro de que deseas eliminar la orden de " + orderToDelete.customerName + "?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            executor.execute(() -> {
                                db.repairOrderDao().delete(orderToDelete);
                                runOnUiThread(() -> {
                                    adapter.removeItem(position);
                                    Snackbar.make(rvOrders, "Orden eliminada", Snackbar.LENGTH_LONG)
                                            .setAction("Deshacer", v -> {
                                                executor.execute(() -> {
                                                    db.repairOrderDao().insert(orderToDelete);
                                                    loadOrdersByFilter(tabLayout.getSelectedTabPosition());
                                                });
                                            }).show();
                                });
                            });
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .setCancelable(false)
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvOrders);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrdersByFilter(tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
