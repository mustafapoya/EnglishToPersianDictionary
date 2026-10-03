package net.golbarg.engtoper.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.engtoper.databinding.ItemExploreWordBinding;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.util.TranslationParser;

import java.util.ArrayList;
import java.util.List;

/** Horizontal "Explore words" carousel on the home screen. */
public class ExploreWordAdapter extends RecyclerView.Adapter<ExploreWordAdapter.ViewHolder> {

    public interface OnWordClickListener {
        void onWordClick(PhraseEnglish word);
    }

    private final List<PhraseEnglish> items = new ArrayList<>();
    private final OnWordClickListener listener;

    public ExploreWordAdapter(OnWordClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PhraseEnglish> words) {
        items.clear();
        if (words != null) items.addAll(words);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemExploreWordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PhraseEnglish word = items.get(position);
        TranslationParser.Parsed parsed = TranslationParser.parse(word.getToLanguage());
        holder.binding.textWord.setText(word.getFromLanguage());
        holder.binding.textMeaning.setText(parsed.joined("، "));
        holder.binding.card.setOnClickListener(v -> listener.onWordClick(word));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExploreWordBinding binding;

        ViewHolder(ItemExploreWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
