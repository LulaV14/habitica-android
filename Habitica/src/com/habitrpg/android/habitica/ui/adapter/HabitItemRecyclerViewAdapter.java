package com.habitrpg.android.habitica.ui.adapter;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.databinding.DailyItemCardBinding;
import com.habitrpg.android.habitica.databinding.HabitItemCardBinding;
import com.habitrpg.android.habitica.databinding.RewardItemCardBinding;
import com.habitrpg.android.habitica.databinding.TodoItemCardBinding;
import com.habitrpg.android.habitica.events.BuyRewardTappedEvent;
import com.habitrpg.android.habitica.events.HabitScoreEvent;
import com.habitrpg.android.habitica.events.TaskLongPressedEvent;
import com.habitrpg.android.habitica.events.TaskSaveEvent;
import com.habitrpg.android.habitica.events.TaskTappedEvent;
import com.habitrpg.android.habitica.events.TaskCheckedEvent;
import com.habitrpg.android.habitica.ui.helpers.ViewHelper;
import com.magicmicky.habitrpgwrapper.lib.models.tasks.ChecklistItem;
import com.magicmicky.habitrpgwrapper.lib.models.tasks.Task;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class HabitItemRecyclerViewAdapter<THabitItem extends Task>
        extends RecyclerView.Adapter<HabitItemRecyclerViewAdapter.ViewHolder>
        implements FlowContentObserver.OnModelStateChangedListener {

    int layoutResource;
    private Class<ViewHolder<THabitItem>> viewHolderClass;
    List<THabitItem> contents;
    Class<THabitItem> taskClass;
    Integer displayedChecklist = null;
    String taskType;
    private ObservableArrayList<THabitItem> observableContent;
    FlowContentObserver observer;
    Context context;

    static final int TYPE_HEADER = 0;
    static final int TYPE_CELL = 1;
    private RecyclerView.Adapter<ViewHolder> parentAdapter;


    public HabitItemRecyclerViewAdapter(String taskType, Class<THabitItem> newTaskClass, int layoutResource, Class<ViewHolder<THabitItem>> viewHolderClass, Context newContext) {
        this(taskType, newTaskClass, layoutResource, viewHolderClass, newContext, null);
    }

    public HabitItemRecyclerViewAdapter(String taskType, Class<THabitItem> newTaskClass, int layoutResource, Class<ViewHolder<THabitItem>> viewHolderClass,
                                        Context newContext, final ObservableArrayList<THabitItem> content) {
        this.taskType = taskType;
        this.context = newContext;
        this.taskClass = newTaskClass;
        observableContent = content;

        if(content == null)
        {
            this.loadContent();

            observer = new FlowContentObserver();
            observer.registerForContentChanges(this.context, this.taskClass);
            observer.addModelChangeListener(this);
        }
        else
        {
            content.addOnListChangedCallback(new ObservableList.OnListChangedCallback() {
                @Override
                public void onChanged(ObservableList sender) {
                    handler.removeCallbacks(reloadContentRunable);
                    handler.postDelayed(reloadContentRunable, 200);
                }

                @Override
                public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
                    handler.removeCallbacks(reloadContentRunable);
                    handler.postDelayed(reloadContentRunable, 200);
                }

                @Override
                public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
                    handler.removeCallbacks(reloadContentRunable);
                    handler.postDelayed(reloadContentRunable, 200);
                }

                @Override
                public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
                    handler.removeCallbacks(reloadContentRunable);
                    handler.postDelayed(reloadContentRunable, 200);
                }

                @Override
                public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
                    handler.removeCallbacks(reloadContentRunable);
                    handler.postDelayed(reloadContentRunable, 200);
                }
            });

            loadContent();
        }

        this.layoutResource = layoutResource;
        this.viewHolderClass = viewHolderClass;
    }

    public void setParentAdapter(RecyclerView.Adapter<HabitItemRecyclerViewAdapter.ViewHolder> parentAdapter)
    {
        this.parentAdapter = parentAdapter;
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        this.observer.unregisterForContentChanges(this.context);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemViewType(int position) {
        switch (position) {
            default:
                return TYPE_CELL;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return contents.size();
    }

    @Override
    public ViewHolder<THabitItem> onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;

        switch (viewType) {
            case TYPE_CELL: {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(layoutResource, parent, false);

                switch (viewHolderClass.getSimpleName()) {
                    case "HabitViewHolder":
                        return new HabitItemRecyclerViewAdapter.HabitViewHolder(view);
                    case "DailyViewHolder":
                        return new HabitItemRecyclerViewAdapter.DailyViewHolder(view);
                    case "TodoViewHolder":
                        return new HabitItemRecyclerViewAdapter.TodoViewHolder(view);
                    case "RewardViewHolder":
                        return new HabitItemRecyclerViewAdapter.RewardViewHolder(view);
                }
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task item = contents.get(position);

        holder.bindHolder(item, position);

        if (this.displayedChecklist != null &&  ChecklistedViewHolder.class.isAssignableFrom(holder.getClass())) {
            ChecklistedViewHolder checklistedHolder = (ChecklistedViewHolder) holder;
            checklistedHolder.setDisplayChecklist(this.displayedChecklist == position);
        }
    }

    private Handler handler = new Handler();
    private Runnable reloadContentRunable = new Runnable() {
        @Override
        public void run() {
            Log.d("Reload Content","");
            loadContent();
        }
    };

    // TODO Move them to a separate class

    @BindingAdapter("bind:imageName")
    public static void loadImage(ImageView view, String imageName) {
        Picasso.with(view.getContext()).load("https://habitica-assets.s3.amazonaws.com/mobileApp/images/shop_"+ imageName +".png").into(view);
    }

    @BindingAdapter("bind:cardColor")
    public static void setCardColor(CardView cardView, int color) {
        if (color > 0) {
            color = cardView.getResources().getColor(color);
        }
        cardView.setCardBackgroundColor(color);
    }

    @BindingAdapter("app:backgroundColor")
    public static void setBackgroundTintColor(CheckBox view, int color) {
        if (color > 0) {
            color = view.getResources().getColor(color);
        }
        ViewHelper.SetBackgroundTint(view, color);
    }

    @BindingAdapter("app:backgroundColor")
    public static void setBackgroundTintColor(Button view, int color) {
        if (color > 0) {
            color = view.getResources().getColor(color);
        }
        ViewHelper.SetBackgroundTint(view, color);
    }

    @BindingAdapter("app:backgroundColor")
    public static void setBackgroundTintColor(View view, int color) {
        if (color > 0) {
            color = view.getResources().getColor(color);
        }
        view.setBackgroundColor(color);
    }

    @BindingAdapter("app:foregroundColor")
    public static void setForegroundTintColor(TextView view, int color) {
        if (color > 0) {
            color = view.getResources().getColor(color);
        }
        view.setTextColor(color);
    }

    @Override
    public void onModelStateChanged(Class<? extends Model> aClass, BaseModel.Action action) {
        handler.removeCallbacks(reloadContentRunable);
        handler.postDelayed(reloadContentRunable, 200);
    }

    public abstract class ViewHolder<THabitItem extends Task> extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        protected android.content.res.Resources resources;

        public THabitItem Item;

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setClickable(true);

            itemView.setOnLongClickListener(this);
            itemView.setLongClickable(true);

            ButterKnife.inject(this, itemView);

            resources = itemView.getResources();
        }

        public void bindHolder(THabitItem habitItem, int position) {
            double itemvalue = habitItem.getValue();
            Item = habitItem;
        }

        @Override
        public void onClick(View v) {
            if (v != itemView)
                return;

            TaskTappedEvent event = new TaskTappedEvent();
            event.Task = Item;

            EventBus.getDefault().post(event);
        }

        @Override
        public boolean onLongClick(View v) {
            TaskLongPressedEvent event = new TaskLongPressedEvent();
            event.Task = Item;

            EventBus.getDefault().post(event);

            return true;
        }

    }

    public class HabitViewHolder extends ViewHolder<Task> {

        @InjectView(R.id.btnPlus)
        Button btnPlus;

        @InjectView(R.id.btnMinus)
        Button btnMinus;

        HabitItemCardBinding binding;

        public HabitViewHolder(View itemView) {
            super(itemView);

            binding = DataBindingUtil.bind(itemView);

            btnPlus.setClickable(true);
            btnPlus.setOnClickListener(this);

            btnMinus.setClickable(true);
            btnMinus.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            HabitScoreEvent event = new HabitScoreEvent();

            if (v == btnPlus) {
                event.Up = true;
                event.Habit = Item;

                EventBus.getDefault().post(event);
            } else if (v == btnMinus) {
                event.Habit = Item;

                EventBus.getDefault().post(event);
            } else super.onClick(v);
        }

        @Override
        public void bindHolder(Task habitItem, int position) {
            super.bindHolder(habitItem, position);

            binding.setHabit(habitItem);
        }
    }

    public class ChecklistedViewHolder extends ViewHolder<Task> implements CompoundButton.OnCheckedChangeListener {

        @InjectView(R.id.checkBox)
        CheckBox checkbox;

        @InjectView(R.id.checklistView)
        LinearLayout checklistView;

        @InjectView(R.id.checklistIndicatorWrapper)
        RelativeLayout checklistIndicatorWrapper;

        public Boolean displayChecklist;

        public ChecklistedViewHolder(View itemView) {
            super(itemView);
            checklistIndicatorWrapper.setOnClickListener(this);
            checkbox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View v) {

            if (v == checklistIndicatorWrapper) {
                if (this.displayChecklist != null) {
                    this.setDisplayChecklist(!this.displayChecklist);
                } else {
                    this.setDisplayChecklist(true);
                }
            } else {
                super.onClick(v);
            }

        }

        @Override
        public void bindHolder(Task habitItem, int position) {
            super.bindHolder(habitItem, position);
            Boolean isClickable = false;
            if (habitItem.getChecklist() != null && habitItem.getChecklist().size() > 0) {
                isClickable = true;
            }
            checklistIndicatorWrapper.setClickable(isClickable);
        }

        public void setDisplayChecklist(Boolean displayChecklist) {
            this.displayChecklist = displayChecklist;
            //This needs to be a LinearLayout, as ListViews can not be inside other ListViews.
            if (this.checklistView != null) {
                if (this.displayChecklist && this.Item.checklist != null) {
                    LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    for (ChecklistItem item : this.Item.checklist) {
                        LinearLayout itemView = (LinearLayout)layoutInflater.inflate(R.layout.checklist_item_row, null);
                        CheckBox checkbox = (CheckBox) itemView.findViewById(R.id.checkBox);
                        checkbox.setOnCheckedChangeListener(this);
                        TextView textView = (TextView) itemView.findViewById(R.id.checkedTextView);
                        // Populate the data into the template view using the data object
                        textView.setText(item.getText());
                        checkbox.setChecked(item.getCompleted());
                        this.checklistView.addView(itemView);
                    }
                } else {
                    this.checklistView.removeAllViewsInLayout();
                }
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == checkbox) {
                if (isChecked != Item.getCompleted()) {
                    TaskCheckedEvent event = new TaskCheckedEvent();
                    event.Task = Item;
                    EventBus.getDefault().post(event);
                }
            } else {
                Integer position = (Integer) ((ViewGroup)checkbox.getParent().getParent()).indexOfChild((View)checkbox.getParent());
                if (isChecked != Item.checklist.get(position).getCompleted()) {
                    TaskSaveEvent event = new TaskSaveEvent();
                    Item.checklist.get(position).setCompleted(isChecked);
                    event.task = Item;
                    EventBus.getDefault().post(event);
                }
            }
        }
    }

    public class DailyViewHolder extends ChecklistedViewHolder {

        DailyItemCardBinding binding;

        public DailyViewHolder(View itemView) {
            super(itemView);

            binding = DataBindingUtil.bind(itemView);

            checkbox.setOnCheckedChangeListener(this);
        }


        @Override
        public void bindHolder(Task habitItem, int position) {
            super.bindHolder(habitItem, position);

            binding.setDaily(habitItem);
        }

        @Override
        public void setDisplayChecklist(Boolean displayChecklist) {
            binding.setDisplayChecklist(displayChecklist);
            super.setDisplayChecklist(displayChecklist);
        }

    }

    public class TodoViewHolder extends ChecklistedViewHolder {

        TodoItemCardBinding binding;

        public TodoViewHolder(View itemView) {
            super(itemView);

            binding = DataBindingUtil.bind(itemView);
        }

        @Override
        public void bindHolder(Task habitItem, int position) {
            super.bindHolder(habitItem, position);

            binding.setTodo(habitItem);
        }

        @Override
        public void setDisplayChecklist(Boolean displayChecklist) {
            binding.setDisplayChecklist(displayChecklist);
            super.setDisplayChecklist(displayChecklist);
        }
    }

    public class RewardViewHolder extends ViewHolder<Task> {
        RewardItemCardBinding binding;

        public RewardViewHolder(View itemView) {
            super(itemView);

            binding = DataBindingUtil.bind(itemView);

            binding.btnReward.setClickable(true);
            binding.btnReward.setOnClickListener(this);
            binding.imageView3.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
            BuyRewardTappedEvent event = new BuyRewardTappedEvent();

            if (v == binding.btnReward) {
                event.Reward = Item;

                EventBus.getDefault().post(event);
            } else super.onClick(v);
        }

        @Override
        public void bindHolder(Task reward, int position) {
            super.bindHolder(reward, position);

            binding.setReward(reward);
        }
    }



    public void loadContent() {
        if(this.observableContent == null) {

            this.contents = new Select().from(this.taskClass)
                    .where(Condition.column("type").eq(this.taskType))
                    .queryList();
        }
        else
        {
            this.contents = observableContent;
        }

        if(parentAdapter != null)
        {
            parentAdapter.notifyDataSetChanged();
        }
        else
        {
            notifyDataSetChanged();
        }
    }
}