package com.example.bardscompanion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;

public class VotingResultAdapter extends RecyclerView.Adapter<VotingResultAdapter.VotingResultViewHolder> {
    private List<VotingResult> votingResults;

    public static class VotingResult {
        public Song song;
        public int voteCount;

        public VotingResult(Song song, int voteCount) {
            this.song = song;
            this.voteCount = voteCount;
        }
    }

    public VotingResultAdapter() {
        this.votingResults = new ArrayList<>();
    }

    @NonNull
    @Override
    public VotingResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voting_result, parent, false);
        return new VotingResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VotingResultViewHolder holder, int position) {
        VotingResult result = votingResults.get(position);
        holder.voteCountText.setText(String.valueOf(result.voteCount));
        holder.votingSongNameText.setText(result.song.getName());
        holder.votingAuthorText.setText(result.song.getAuthor());
    }

    @Override
    public int getItemCount() {
        return votingResults.size();
    }

    public void updateVotingResults(List<VotingResult> newResults) {
        this.votingResults = newResults;
        notifyDataSetChanged();
    }

    static class VotingResultViewHolder extends RecyclerView.ViewHolder {
        TextView voteCountText;
        TextView votingSongNameText;
        TextView votingAuthorText;

        VotingResultViewHolder(@NonNull View itemView) {
            super(itemView);
            voteCountText = itemView.findViewById(R.id.voteCountText);
            votingSongNameText = itemView.findViewById(R.id.votingSongNameText);
            votingAuthorText = itemView.findViewById(R.id.votingAuthorText);
        }
    }
}