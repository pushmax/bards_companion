package com.example.bardscompanion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;

public class ReactionResultAdapter extends RecyclerView.Adapter<ReactionResultAdapter.ReactionResultViewHolder> {
    private List<ReactionResult> reactionResults;

    public static class ReactionResult {
        public String reactionType;
        public int reactionCount;
        public String emoji;
        public String displayName;

        public ReactionResult(String reactionType, int reactionCount) {
            this.reactionType = reactionType;
            this.reactionCount = reactionCount;
            
            switch (reactionType) {
                case "panties":
                    this.emoji = "👙";
                    this.displayName = "Panties";
                    break;
                case "heart":
                    this.emoji = "❤️";
                    this.displayName = "Hearts";
                    break;
                case "tomato":
                    this.emoji = "🍅";
                    this.displayName = "Tomatoes";
                    break;
                case "shutup":
                    this.emoji = "🤫";
                    this.displayName = "Shut ups";
                    break;
                default:
                    this.emoji = "🎭";
                    this.displayName = reactionType;
                    break;
            }
        }
    }

    public ReactionResultAdapter() {
        this.reactionResults = new ArrayList<>();
    }

    @NonNull
    @Override
    public ReactionResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reaction_result, parent, false);
        return new ReactionResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReactionResultViewHolder holder, int position) {
        ReactionResult result = reactionResults.get(position);
        holder.reactionCountText.setText(String.valueOf(result.reactionCount));
        holder.reactionEmojiText.setText(result.emoji);
        holder.reactionNameText.setText(result.displayName);
    }

    @Override
    public int getItemCount() {
        return reactionResults.size();
    }

    public void updateReactionResults(List<ReactionResult> newResults) {
        this.reactionResults = newResults;
        notifyDataSetChanged();
    }

    static class ReactionResultViewHolder extends RecyclerView.ViewHolder {
        TextView reactionCountText;
        TextView reactionEmojiText;
        TextView reactionNameText;

        ReactionResultViewHolder(@NonNull View itemView) {
            super(itemView);
            reactionCountText = itemView.findViewById(R.id.reactionCountText);
            reactionEmojiText = itemView.findViewById(R.id.reactionEmojiText);
            reactionNameText = itemView.findViewById(R.id.reactionNameText);
        }
    }
}