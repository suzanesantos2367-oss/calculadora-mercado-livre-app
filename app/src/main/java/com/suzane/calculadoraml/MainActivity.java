package com.suzane.calculadoraml;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BLUE = Color.rgb(13,70,160);
    private final int BLUE_DARK = Color.rgb(8,45,104);
    private final int YELLOW = Color.rgb(255,217,0);
    private final int LIGHT = Color.rgb(244,247,251);

    private ProductDatabase db;
    private EditText nameInput, taxInput, extraInput, costInput, searchInput;
    private LinearLayout productList, resultList;
    private TextView statusText, statsText;
    private Button favoriteFilterButton, saveButton;
    private boolean favoritesOnly = false;
    private long editingId = 0;
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ProductDatabase(this);
        setContentView(buildScreen());
        refreshProducts();
        calculate();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = vertical();
        root.setBackgroundColor(Color.WHITE);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setPadding(dp(20), dp(18), dp(20), dp(18));
        header.setBackgroundColor(BLUE_DARK);
        TextView title = text("🧮  CALCULADORA MERCADO LIVRE", 23, Color.WHITE, true);
        TextView sub = text("Precificação, lucro e produtos salvos", 16, Color.WHITE, false);
        header.addView(title); header.addView(sub);
        root.addView(header, matchWrap());
        View yellow = new View(this); yellow.setBackgroundColor(YELLOW);
        root.addView(yellow, new LinearLayout.LayoutParams(-1, dp(5)));

        LinearLayout content = vertical();
        content.setPadding(dp(14),dp(14),dp(14),dp(30));
        root.addView(content, matchWrap());

        LinearLayout form = card();
        form.addView(sectionTitle("📦 Consulta do Produto"));
        nameInput = field(form, "NOME DO PRODUTO", "Ex.: Mini Processador USB", false);
        taxInput = field(form, "IMPOSTO / TAXA (%)", "0", true);
        extraInput = field(form, "CUSTO EXTRA (R$)", "0,00", true);
        costInput = field(form, "CUSTO TOTAL DO PRODUTO (R$)", "0,00", true);

        LinearLayout actions = horizontalWrap();
        saveButton = button("💾 Salvar produto", BLUE, Color.WHITE);
        Button newBtn = button("＋ Nova consulta", Color.WHITE, BLUE);
        Button clearBtn = button("↻ Limpar valores", Color.WHITE, BLUE);
        actions.addView(saveButton); actions.addView(newBtn); actions.addView(clearBtn);
        form.addView(actions);
        statusText = text("Nova consulta", 14, BLUE, true);
        statusText.setPadding(0,dp(10),0,0);
        form.addView(statusText);
        content.addView(form, matchWrap());

        LinearLayout products = card();
        products.addView(sectionTitle("📚 Meus Produtos"));
        searchInput = new EditText(this);
        searchInput.setHint("Pesquisar produto...");
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(12),dp(10),dp(12),dp(10));
        products.addView(searchInput, matchWrap());
        favoriteFilterButton = button("⭐ Apenas favoritos", Color.rgb(255,244,191), Color.rgb(122,89,0));
        products.addView(favoriteFilterButton);
        statsText = text("",14,Color.DKGRAY,true);
        statsText.setPadding(0,dp(10),0,dp(10));
        products.addView(statsText);
        productList = vertical();
        products.addView(productList, matchWrap());
        content.addView(products, matchWrap());

        LinearLayout resultsCard = card();
        resultsCard.addView(sectionTitle("📊 Resultado da Consulta"));
        resultList = vertical();
        resultsCard.addView(resultList, matchWrap());
        content.addView(resultsCard, matchWrap());

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ calculate(); }
            public void afterTextChanged(Editable e){}
        };
        taxInput.addTextChangedListener(watcher);
        extraInput.addTextChangedListener(watcher);
        costInput.addTextChangedListener(watcher);

        saveButton.setOnClickListener(v -> saveProduct());
        newBtn.setOnClickListener(v -> newConsult());
        clearBtn.setOnClickListener(v -> { taxInput.setText("0"); extraInput.setText("0,00"); costInput.setText("0,00"); });
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ refreshProducts(); }
            public void afterTextChanged(Editable e){}
        });
        favoriteFilterButton.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            favoriteFilterButton.setText(favoritesOnly ? "⭐ Mostrar todos" : "⭐ Apenas favoritos");
            refreshProducts();
        });
        return scroll;
    }

    private void saveProduct() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            toast("Digite o nome do produto.");
            nameInput.requestFocus();
            return;
        }
        Product p = editingId == 0 ? new Product() : db.get(editingId);
        if (p == null) p = new Product();
        p.name = name;
        p.cost = number(costInput);
        p.extra = number(extraInput);
        p.tax = Math.min(99.99, Math.max(0, number(taxInput)));
        p.updatedAt = System.currentTimeMillis();
        if (editingId == 0) {
            editingId = db.insert(p);
        } else {
            p.id = editingId;
            db.update(p);
        }
        saveButton.setText("💾 Atualizar produto");
        statusText.setText("Editando: " + p.name);
        toast("Produto salvo com sucesso.");
        refreshProducts();
    }

    private void loadProduct(Product p) {
        editingId = p.id;
        nameInput.setText(p.name);
        costInput.setText(formatPlain(p.cost));
        extraInput.setText(formatPlain(p.extra));
        taxInput.setText(formatPlain(p.tax));
        saveButton.setText("💾 Atualizar produto");
        statusText.setText("Editando: " + p.name);
        scrollTop();
    }

    private void newConsult() {
        editingId = 0;
        nameInput.setText("");
        taxInput.setText("0");
        extraInput.setText("0,00");
        costInput.setText("0,00");
        saveButton.setText("💾 Salvar produto");
        statusText.setText("Nova consulta");
        nameInput.requestFocus();
        scrollTop();
    }

    private void refreshProducts() {
        if (productList == null) return;
        productList.removeAllViews();
        List<Product> products = db.search(searchInput == null ? "" : searchInput.getText().toString(), favoritesOnly);
        statsText.setText("Produtos salvos: " + db.countAll() + "   •   Favoritos: " + db.countFavorites());
        if (products.isEmpty()) {
            TextView empty = text("Nenhum produto encontrado.", 15, Color.GRAY, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0,dp(18),0,dp(18));
            productList.addView(empty);
            return;
        }
        for (Product p : products) productList.addView(productCard(p), matchWrap());
    }

    private View productCard(Product p) {
        LinearLayout box = vertical();
        box.setPadding(dp(12),dp(10),dp(12),dp(10));
        box.setBackgroundColor(LIGHT);
        LinearLayout.LayoutParams bp = matchWrap();
        bp.setMargins(0,0,0,dp(9));
        box.setLayoutParams(bp);

        TextView title = text((p.favorite ? "⭐ " : "☆ ") + p.name, 18, BLUE, true);
        title.setOnClickListener(v -> toggleFavorite(p));
        box.addView(title);

        String meta = "Custo: " + money.format(p.cost) + "  •  Extra: " + money.format(p.extra) +
                "  •  Taxa: " + formatPlain(p.tax) + "%";
        box.addView(text(meta, 13, Color.DKGRAY, false));
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt","BR")).format(new Date(p.updatedAt));
        box.addView(text("Atualizado: " + date, 12, Color.GRAY, false));

        LinearLayout actions = horizontalWrap();
        Button open = button("Abrir", BLUE, Color.WHITE);
        Button rename = button("Renomear", Color.WHITE, BLUE);
        Button delete = button("Excluir", Color.WHITE, Color.rgb(198,40,40));
        actions.addView(open); actions.addView(rename); actions.addView(delete);
        box.addView(actions);

        open.setOnClickListener(v -> loadProduct(p));
        rename.setOnClickListener(v -> renameProduct(p));
        delete.setOnClickListener(v -> confirmDelete(p));
        return box;
    }

    private void toggleFavorite(Product p) {
        p.favorite = !p.favorite;
        p.updatedAt = System.currentTimeMillis();
        db.update(p);
        refreshProducts();
    }

    private void renameProduct(Product p) {
        final EditText input = new EditText(this);
        input.setText(p.name);
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Renomear produto")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (d,w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        p.name = name;
                        p.updatedAt = System.currentTimeMillis();
                        db.update(p);
                        if (editingId == p.id) {
                            nameInput.setText(name);
                            statusText.setText("Editando: " + name);
                        }
                        refreshProducts();
                    }
                }).show();
    }

    private void confirmDelete(Product p) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir produto")
                .setMessage("Deseja excluir \"" + p.name + "\"?")
                .setNegativeButton("Não", null)
                .setPositiveButton("Sim", (d,w) -> {
                    db.delete(p.id);
                    if (editingId == p.id) newConsult();
                    refreshProducts();
                }).show();
    }

    private void calculate() {
        if (resultList == null) return;
        resultList.removeAllViews();
        double total = Math.max(0, number(costInput)) + Math.max(0, number(extraInput));
        double tax = Math.min(99.99, Math.max(0, number(taxInput))) / 100.0;

        LinearLayout header = horizontal();
        header.setBackgroundColor(BLUE);
        header.addView(cell("LUCRO", Color.WHITE, true));
        header.addView(cell("VOCÊ RECEBE", Color.WHITE, true));
        header.addView(cell("SEU LUCRO", Color.WHITE, true));
        resultList.addView(header);

        for (int percent=0; percent<=100; percent+=5) {
            double profit = total * percent / 100.0;
            double receive = tax > 0 ? (total + profit) / (1.0 - tax) : total + profit;
            LinearLayout row = horizontal();
            row.setBackgroundColor(percent % 10 == 0 ? Color.WHITE : LIGHT);
            row.addView(cell(percent + "%", BLUE, true));
            row.addView(cell(money.format(receive), Color.DKGRAY, true));
            row.addView(cell(money.format(profit), Color.rgb(47,143,24), true));
            resultList.addView(row);
        }
    }

    private EditText field(LinearLayout parent, String label, String hint, boolean decimal) {
        TextView l = text(label, 15, BLUE, true);
        l.setPadding(0,dp(8),0,dp(4));
        parent.addView(l);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(18);
        e.setPadding(dp(12),dp(8),dp(12),dp(8));
        if (decimal) e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        parent.addView(e, matchWrap());
        return e;
    }

    private LinearLayout card() {
        LinearLayout l = vertical();
        l.setPadding(dp(14),dp(14),dp(14),dp(14));
        l.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(0,0,0,dp(14));
        l.setLayoutParams(p);
        return l;
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 21, BLUE, true);
        t.setPadding(0,0,0,dp(8));
        return t;
    }

    private Button button(String s, int bg, int fg) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(fg); b.setBackgroundColor(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(48));
        p.setMargins(0,dp(8),dp(8),0);
        b.setLayoutParams(p);
        return b;
    }

    private TextView cell(String s, int color, boolean bold) {
        TextView t = text(s, 13, color, bold);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(4),dp(10),dp(4),dp(10));
        t.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout horizontalWrap() {
        LinearLayout l = horizontal();
        l.setGravity(Gravity.START);
        return l;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1,-2);
    }

    private double number(EditText e) {
        if (e == null) return 0;
        String s = e.getText().toString().trim().replace("R$","").replace(" ","");
        if (s.contains(",") && s.contains(".")) s = s.replace(".","").replace(",",".");
        else s = s.replace(",",".");
        try { return Double.parseDouble(s); } catch (Exception ex) { return 0; }
    }

    private String formatPlain(double v) {
        return String.format(new Locale("pt","BR"), "%.2f", v);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void scrollTop() {
        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            View child = ((ViewGroup) root).getChildAt(0);
            if (child instanceof ScrollView) ((ScrollView) child).smoothScrollTo(0,0);
        }
    }
}
