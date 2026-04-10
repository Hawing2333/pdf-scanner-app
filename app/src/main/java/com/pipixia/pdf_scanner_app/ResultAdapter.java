package com.pipixia.pdf_scanner_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int pageNumber);
    }

    private final List<SearchResult> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SearchResult> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = items.get(position);
        holder.tvBadge.setText(String.valueOf(result.pageNumber));
        holder.tvContext.setText(result.context);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(result.pageNumber);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvBadge;
        final TextView tvContext;

        ViewHolder(View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_page_badge);
            tvContext = itemView.findViewById(R.id.tv_context);
        }
    }
}
