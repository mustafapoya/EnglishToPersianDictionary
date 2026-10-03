package net.golbarg.engtoper.ui.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.engtoper.databinding.ItemPhraseBinding;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.ui.common.PhraseItemBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhraseEnglishAdapter extends RecyclerView.Adapter<PhraseEnglishAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PhraseEnglish phrase);
    }

    public interface OnSpeakerClickListener {
        void onSpeakerClick(PhraseEnglish phrase);
    }

    public interface OnBookmarkClickListener {
        void onBookmarkClick(PhraseEnglish phrase, int position);
    }

    private final List<PhraseEnglish> items = new ArrayList<>();
    private final PhraseItemBinder binder = new PhraseItemBinder();
    private final OnItemClickListener itemClickListener;
    private final OnSpeakerClickListener speakerClickListener;
    private final OnBookmarkClickListener bookmarkClickListener;

    public PhraseEnglishAdapter(
            OnItemClickListener itemClickListener,
            OnSpeakerClickListener speakerClickListener,
            OnBookmarkClickListener bookmarkClickListener
    ) {
        this.itemClickListener = itemClickListener;
        this.speakerClickListener = speakerClickListener;
        this.bookmarkClickListener = bookmarkClickListener;
    }

    public void submitList(List<PhraseEnglish> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems != null ? newItems.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).getId() == (newItems != null ? newItems.get(newItemPosition).getId() : -1);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                if (newItems == null) return false;
                PhraseEnglish oldItem = items.get(oldItemPosition);
                PhraseEnglish newItem = newItems.get(newItemPosition);
                return oldItem.getFavorite() == newItem.getFavorite()
                        && Objects.equals(oldItem.getFromLanguage(), newItem.getFromLanguage())
                        && Objects.equals(oldItem.getToLanguage(), newItem.getToLanguage());
            }
        });

        items.clear();
        binder.clearCache();
        if (newItems != null) {
            items.addAll(newItems);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhraseBinding binding = ItemPhraseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPhraseBinding binding;

        public ViewHolder(ItemPhraseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PhraseEnglish phrase) {
            binder.bind(binding, phrase.getId(), phrase.getFromLanguage(), phrase.getToLanguage(),
                    phrase.getFavorite() == 1, true);

            // Click Listeners
            binding.cardWord.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(phrase);
                }
            });

            binding.btnSpeaker.setOnClickListener(v -> {
                if (speakerClickListener != null) {
                    speakerClickListener.onSpeakerClick(phrase);
                }
            });

            binding.btnBookmark.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && bookmarkClickListener != null) {
                    bookmarkClickListener.onBookmarkClick(phrase, position);
                }
            });
        }
    }
}
