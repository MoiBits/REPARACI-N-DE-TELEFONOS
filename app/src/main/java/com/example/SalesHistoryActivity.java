package com.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesHistoryActivity extends AppCompatActivity {

    private RecyclerView rvSales;
    private SalesAdapter adapter;
    private AppDatabase db;
    private SearchView searchView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        db = AppDatabase.getInstance(this);
        initViews();
        loadSales();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSales = findViewById(R.id.rvSales);
        searchView = findViewById(R.id.searchView);

        rvSales.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalesAdapter();
        rvSales.setAdapter(adapter);

        findViewById(R.id.fabNewSale).setOnClickListener(v -> {
            startActivity(new Intent(this, PhoneSaleActivity.class));
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSales(newText);
                return true;
            }
        });
    }

    private void loadSales() {
        executor.execute(() -> {
            List<PhoneSale> sales = db.phoneSaleDao().getAllSales();
            runOnUiThread(() -> adapter.setSales(sales));
        });
    }

    private void filterSales(String query) {
        executor.execute(() -> {
            List<PhoneSale> allSales = db.phoneSaleDao().getAllSales();
            if (query == null || query.trim().isEmpty()) {
                runOnUiThread(() -> adapter.setSales(allSales));
                return;
            }
            List<PhoneSale> filtered = new ArrayList<>();
            for (PhoneSale sale : allSales) {
                if ((sale.customerName != null && sale.customerName.toLowerCase().contains(query.toLowerCase())) ||
                    (sale.deviceModel != null && sale.deviceModel.toLowerCase().contains(query.toLowerCase())) ||
                    (sale.imei != null && sale.imei.contains(query))) {
                    filtered.add(sale);
                }
            }
            runOnUiThread(() -> adapter.setSales(filtered));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSales();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private static class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.ViewHolder> {
        private List<PhoneSale> sales = new ArrayList<>();

        public void setSales(List<PhoneSale> sales) {
            this.sales = sales;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_phone_sale, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PhoneSale sale = sales.get(position);
            Context context = holder.itemView.getContext();

            String fullModel = (sale.deviceBrand != null ? sale.deviceBrand : "") + " " + (sale.deviceModel != null ? sale.deviceModel : "");
            holder.tvModel.setText(fullModel.trim());
            holder.tvCustomer.setText("Comprador: " + sale.customerName);
            
            String currency = PreferenceManager.getCurrencySymbol(context);
            holder.tvPrice.setText(currency + " " + String.format(Locale.getDefault(), "%.2f", sale.price));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(sale.saleDate)));
            holder.tvCondition.setText(sale.condition);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, SaleReceiptActivity.class);
                intent.putExtra("sale_id", sale.id);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return sales.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvModel, tvCustomer, tvPrice, tvDate, tvCondition;

            ViewHolder(View v) {
                super(v);
                tvModel = v.findViewById(R.id.tvSaleModel);
                tvCustomer = v.findViewById(R.id.tvSaleCustomer);
                tvPrice = v.findViewById(R.id.tvSalePrice);
                tvDate = v.findViewById(R.id.tvSaleDate);
                tvCondition = v.findViewById(R.id.tvSaleCondition);
            }
        }
    }
}
