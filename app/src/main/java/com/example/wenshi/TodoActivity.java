package com.example.wenshi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wenshi.data.AppDatabase;
import com.example.wenshi.data.TodoItem;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodoActivity extends AppCompatActivity {
    private LinearLayout todoContainer;
    private Button addTodoButton;
    private View enterButton;
    private boolean isEnterButtonVisible = false;
    private Handler handler;
    private ExecutorService executor;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        initializeComponents();
        setupViews();
        loadTodos();
        startCountdownUpdates();
    }

    private void initializeComponents() {
        handler = new Handler();
        executor = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(this);
    }

    private void setupViews() {
        todoContainer = findViewById(R.id.todoContainer);
        addTodoButton = findViewById(R.id.addTodoButton);
        enterButton = findViewById(R.id.enterButton);

        // 设置初始状态：进入按钮隐藏
        enterButton.setVisibility(View.INVISIBLE);
        enterButton.setAlpha(0f);
        enterButton.setScaleX(0.8f);
        enterButton.setScaleY(0.8f);

        // 设置整个界面的点击监听
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> toggleEnterButton());

        // 设置进入按钮点击监听
        enterButton.setOnClickListener(v -> {
            showTodoInterface();
            hideEnterButton();
        });

        addTodoButton.setOnClickListener(v -> showAddTodoDialog());
    }

    private void toggleEnterButton() {
        if (isEnterButtonVisible) {
            hideEnterButton();
        } else {
            showEnterButton();
        }
    }

    private void showEnterButton() {
        enterButton.setVisibility(View.VISIBLE);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(enterButton, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(enterButton, "scaleX", 0.8f, 1f),
                ObjectAnimator.ofFloat(enterButton, "scaleY", 0.8f, 1f)
        );
        animatorSet.setDuration(200);
        animatorSet.start();
        isEnterButtonVisible = true;
    }

    // 修改 hideEnterButton 方法
    private void hideEnterButton() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(enterButton, "alpha", 1f, 0f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(enterButton, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(enterButton, "scaleY", 1f, 0.8f);

        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
        animatorSet.setDuration(200);

        // 使用 AnimatorListenerAdapter 替换 withEndAction
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                enterButton.setVisibility(View.INVISIBLE);
                isEnterButtonVisible = false;
            }
        });

        animatorSet.start();
    }

    private void showTodoInterface() {
        View todoInterface = findViewById(R.id.todoInterface);
        todoInterface.setVisibility(View.VISIBLE);
        todoInterface.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void showAddTodoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_todo, null);
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        TextView dateTimeText = dialogView.findViewById(R.id.dateTimeText);
        Button selectDateTime = dialogView.findViewById(R.id.selectDateTime);

        final Calendar calendar = Calendar.getInstance();

        selectDateTime.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("选择日期")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                calendar.setTimeInMillis(selection);

                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                        .setMinute(calendar.get(Calendar.MINUTE))
                        .setTitleText("选择时间")
                        .build();

                timePicker.addOnPositiveButtonClickListener(view -> {
                    calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                    calendar.set(Calendar.MINUTE, timePicker.getMinute());
                    dateTimeText.setText(String.format(Locale.getDefault(),
                            "%tF %tR", calendar, calendar));
                });

                timePicker.show(getSupportFragmentManager(), "timePicker");
            });

            datePicker.show(getSupportFragmentManager(), "datePicker");
        });

        // ... 显示对话框并处理确认按钮点击事件 ...
    }

    private void loadTodos() {
        executor.execute(() -> {
            List<TodoItem> todos = database.todoDao().getAllTodos();
            handler.post(() -> {
                todoContainer.removeAllViews();
                for (TodoItem todo : todos) {
                    addTodoItemView(todo);
                }
            });
        });
    }

    private void addTodoItemView(TodoItem todo) {
        View itemView = getLayoutInflater().inflate(R.layout.item_todo, null);
        TextView titleText = itemView.findViewById(R.id.todoTitle);
        TextView countdownText = itemView.findViewById(R.id.todoCountdown);

        titleText.setText(todo.title);
        updateCountdown(countdownText, todo.targetTimestamp);

        todoContainer.addView(itemView);
    }

    private void updateCountdown(TextView countdownText, long targetTimestamp) {
        long currentTime = System.currentTimeMillis();
        long timeLeft = targetTimestamp - currentTime;

        if (timeLeft <= 0) {
            countdownText.setText("已过期");
            return;
        }

        long days = timeLeft / (24 * 60 * 60 * 1000);
        timeLeft %= (24 * 60 * 60 * 1000);
        long hours = timeLeft / (60 * 60 * 1000);
        timeLeft %= (60 * 60 * 1000);
        long minutes = timeLeft / (60 * 1000);

        String countdownStr = String.format(Locale.getDefault(),
                "%d天%d小时%d分钟", days, hours, minutes);
        countdownText.setText(countdownStr);
    }

    private void startCountdownUpdates() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < todoContainer.getChildCount(); i++) {
                    View itemView = todoContainer.getChildAt(i);
                    TextView countdownText = itemView.findViewById(R.id.todoCountdown);
                    Object tag = itemView.getTag();
                    if (tag instanceof TodoItem) {
                        updateCountdown(countdownText, ((TodoItem) tag).targetTimestamp);
                    }
                }
                handler.postDelayed(this, 60000); // 每分钟更新一次
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        handler.removeCallbacksAndMessages(null);
    }
}