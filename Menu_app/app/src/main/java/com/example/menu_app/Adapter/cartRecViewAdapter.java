package com.example.menu_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_app.R;
import com.example.menu_app.classes.dish;

import java.util.ArrayList;

public class cartRecViewAdapter extends RecyclerView.Adapter<cartRecViewAdapter.ViewHolder>{

    private final ArrayList<dish> cart = new ArrayList<dish>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_dishes_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return cart.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView cart_dishes;
        private final TextView dish_price_cart;
        //private final TextView totalPrice;
        private final RelativeLayout cart_parent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cart_dishes = itemView.findViewById(R.id.cart_dishes);
            dish_price_cart = itemView.findViewById(R.id.dish_price_cart);
            cart_parent = itemView.findViewById(R.id.cart_parent);
            //totalPrice = itemView.findViewById(R.id.totalPrice);

        }
    }
}
