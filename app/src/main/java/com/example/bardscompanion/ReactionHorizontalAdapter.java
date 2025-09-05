package com.example.bardscompanion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;

public class ReactionHorizontalAdapter extends RecyclerView.Adapter<ReactionHorizontalAdapter.ReactionHorizontalViewHolder> {
    private List<ReactionResult> reactionResults;

    public static class ReactionResult {
        public String reactionType;
        public int reactionCount;
        public String emoji;

        public ReactionResult(String reactionType, int reactionCount) {
            this.reactionType = reactionType;
            this.reactionCount = reactionCount;
            
            switch (reactionType) {
                case "panties":
                    this.emoji = "👙";
                    break;
                case "heart":
                    this.emoji = "❤️";
                    break;
                case "tomato":
                    this.emoji = "🍅";
                    break;
                case "shutup":
                    this.emoji = "🤫";
                    break;
                default:
                    this.emoji = "🎭";
                    break;
            }
        }
    }

    public ReactionHorizontalAdapter() {
        this.reactionResults = new ArrayList<>();
    }

    @NonNull
    @Override
    public ReactionHorizontalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reaction_horizontal, parent, false);
        return new ReactionHorizontalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReactionHorizontalViewHolder holder, int position) {
        ReactionResult result = reactionResults.get(position);
        holder.reactionCountText.setText(String.valueOf(result.reactionCount));
        holder.reactionEmojiText.setText(result.emoji);
    }

    @Override
    public int getItemCount() {
        return reactionResults.size();
    }

    public void updateReactionResults(List<ReactionResult> newResults) {
        this.reactionResults = newResults;
        notifyDataSetChanged();
    }

    static class ReactionHorizontalViewHolder extends RecyclerView.ViewHolder {
        TextView reactionCountText;
        TextView reactionEmojiText;

        ReactionHorizontalViewHolder(@NonNull View itemView) {
            super(itemView);
            reactionCountText = itemView.findViewById(R.id.reactionCountText);
            reactionEmojiText = itemView.findViewById(R.id.reactionEmojiText);
        }
    }
}