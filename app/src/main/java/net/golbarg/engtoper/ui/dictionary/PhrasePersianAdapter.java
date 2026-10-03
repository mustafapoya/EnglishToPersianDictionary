package net.golbarg.engtoper.ui.dictionary;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.engtoper.databinding.ItemPhraseBinding;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.ui.common.PhraseItemBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhrasePersianAdapter extends RecyclerView.Adapter<PhrasePersianAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PhrasePersian phrase);
    }

    public interface OnBookmarkClickListener {
        void onBookmarkClick(PhrasePersian phrase, int position);
    }

    private final List<PhrasePersian> items = new ArrayList<>();
    private final PhraseItemBinder binder = new PhraseItemBinder();
    private final OnItemClickListener itemClickListener;
    private final OnBookmarkClickListener bookmarkClickListener;

    public PhrasePersianAdapter(
            OnItemClickListener itemClickListener,
            OnBookmarkClickListener bookmarkClickListener
    ) {
        this.itemClickListener = itemClickListener;
        this.bookmarkClickListener = bookmarkClickListener;
    }

    public void submitList(List<PhrasePersian> newItems) {
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
                PhrasePersian oldItem = items.get(oldItemPosition);
                PhrasePersian newItem = newItems.get(newItemPosition);
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

        public void bind(PhrasePersian phrase) {
            binder.bind(binding, phrase.getId(), phrase.getFromLanguage(), phrase.getToLanguage(),
                    phrase.getFavorite() == 1, false);

            binding.cardWord.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(phrase);
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
