package ru.tileclock;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int INK = Color.rgb(16,31,69), BLUE = Color.rgb(20,89,219), CREAM = Color.rgb(248,240,229), RED = Color.rgb(202,53,48);
    private final String[] avatars = {"🦊","🐻","🐱","🐼","🐶","🐨","🐯","🦁","🐸","🐰","🐟","🐏"};
    private final String[] animalNames = {"Лиса","Медведь","Кот","Панда","Собака","Коала","Тигр","Лев","Лягушка","Кролик","Рыбка","Барашек"};
    private String[] names = {"Анна","Миша","Катя","Саша"};
    private int[] faces = {0,1,2,3};
    private int count = 4, duration = 60, previousSecond = -1;
    private boolean sound = true, game = false;
    private LinearLayout playerStrip;
    private GameLayout gameLayout;
    private TextView headline, round, next, hint, pauseButton;
    private TimerTile tile;
    private TurnTimer timer;
    private ToneGenerator tones;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!game || timer == null) return;
            long now = SystemClock.elapsedRealtime();
            boolean expired = timer.advanceIfExpired(now);
            int seconds = timer.displayedSeconds(now);
            if (expired) beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700);
            else if (timer.state == TurnTimer.State.RUNNING && seconds != previousSecond) {
                if (seconds == 10) beep(ToneGenerator.TONE_PROP_BEEP2, 220);
                else if (seconds > 0 && seconds < 10) beep(ToneGenerator.TONE_PROP_BEEP, 65);
            }
            previousSecond = seconds;
            tile.invalidate();
            if (expired) { refreshGame(); persist(); }
            handler.postDelayed(this, 80);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(CREAM); getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        try { tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 75); } catch (RuntimeException ignored) { }
        android.content.SharedPreferences p = getSharedPreferences("game", MODE_PRIVATE);
        count = Math.max(2, Math.min(4, p.getInt("count",4))); duration = p.getInt("duration",60);
        if (duration != 30 && duration != 60 && duration != 90 && duration != 120) duration = 60;
        sound = p.getBoolean("sound",true);
        for (int i=0;i<4;i++) { names[i]=p.getString("name"+i,names[i]); faces[i]=Math.max(0,Math.min(avatars.length-1,p.getInt("face"+i,i))); }
        game=p.getBoolean("game",false);
        if (game) {
            timer=new TurnTimer(count,duration);
            TurnTimer.State state;
            try { state=TurnTimer.State.valueOf(p.getString("state","PAUSED")); } catch (Exception e) { state=TurnTimer.State.PAUSED; }
            timer.restore(p.getInt("player",0),p.getInt("turn",1),state,p.getLong("remaining",duration*1000L));
            showGame();
        } else showSetup();
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int color, int radius, boolean border) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{color,color});
        d.setCornerRadius(dp(radius)); if(border)d.setStroke(dp(1),Color.rgb(224,210,191)); return d;
    }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String s,int size,boolean bold) {
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(INK);
        if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    private TextView button(String s,boolean primary,Runnable action) {
        TextView v=text(s,16,true); v.setGravity(Gravity.CENTER); v.setPadding(dp(10),dp(14),dp(10),dp(14));
        v.setBackground(bg(primary?BLUE:Color.rgb(255,249,240),17,!primary)); v.setTextColor(primary?Color.WHITE:INK);
        fitText(v,11,16); v.setElevation(dp(2)); v.setOnClickListener(w->action.run()); v.setFocusable(true); return v;
    }
    private void fitText(TextView v,int min,int max) {
        v.setAutoSizeTextTypeUniformWithConfiguration(min,max,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    }
    private void baseGame() {
        gameLayout=new GameLayout(); gameLayout.setBackgroundColor(CREAM);
        gameLayout.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            }
            return insets;
        });
        setContentView(gameLayout); gameLayout.requestApplyInsets();
    }
    private void addGame(View v) { gameLayout.addView(v); }
    /** Measures against the usable window, so the footer never scrolls offscreen. */
    private class GameLayout extends ViewGroup {
        private GameLayoutPlan.Box[] boxes;
        GameLayout() { super(MainActivity.this); }
        @Override protected void onMeasure(int widthSpec,int heightSpec) {
            int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);
            float density=getResources().getDisplayMetrics().density;
            int usableWidth=width-getPaddingLeft()-getPaddingRight();
            int usableHeight=height-getPaddingTop()-getPaddingBottom();
            boxes=GameLayoutPlan.arrange(usableWidth/density,usableHeight/density);
            for(int i=0;i<getChildCount();i++) {
                GameLayoutPlan.Box b=boxes[i];
                int w=Math.round(b.right*density)-Math.round(b.left*density);
                int h=Math.round(b.bottom*density)-Math.round(b.top*density);
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(width,height);
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
            float d=getResources().getDisplayMetrics().density;
            for(int i=0;i<getChildCount();i++) {
                GameLayoutPlan.Box box=boxes[i];
                getChildAt(i).layout(getPaddingLeft()+Math.round(box.left*d),getPaddingTop()+Math.round(box.top*d),getPaddingLeft()+Math.round(box.right*d),getPaddingTop()+Math.round(box.bottom*d));
            }
        }
    }
    private void showSetup() {
        handler.removeCallbacks(ticker); game=false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        SetupLayout setup=new SetupLayout(); setup.setBackgroundColor(CREAM);
        setup.setFocusableInTouchMode(true);
        setup.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());
                setup.keyboard=insets.isVisible(WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
                v.requestLayout();
            }
            return insets;
        });
        LinearLayout heading=column(); heading.setGravity(Gravity.CENTER);
        TextView title=text("Румикуб",30,true); title.setGravity(Gravity.CENTER); fitText(title,22,30);
        heading.addView(title,new LinearLayout.LayoutParams(-1,0,2));
        TextView subtitle=text("Таймер для вашей компании",12,false); subtitle.setGravity(Gravity.CENTER); fitText(subtitle,10,12);
        heading.addView(subtitle,new LinearLayout.LayoutParams(-1,0,1)); setup.addView(heading);
        LinearLayout choices=row(); TextView label=text("Игроки",17,true); fitText(label,12,17);
        choices.addView(label,new LinearLayout.LayoutParams(0,-1,1.4f));
        for(int n=2;n<=4;n++) {
            final int num=n; TextView b=button(""+n,n==count,()->{ count=num; persist(); showSetup(); });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); lp.setMargins(dp(3),0,dp(3),0); choices.addView(b,lp);
        }
        setup.addView(choices);
        ScrollView players=new ScrollView(this); players.setFillViewport(false);
        setup.grid=new PlayerGrid(); players.addView(setup.grid); setup.addView(players);
        for(int i=0;i<count;i++) {
            final int index=i; LinearLayout r=row(); r.setPadding(dp(2),0,dp(4),0);
            r.setBackground(bg(0xfffff9f0,16,true));
            TextView avatar=text(avatars[faces[i]],28,false); avatar.setGravity(Gravity.CENTER);
            avatar.setContentDescription("Аватар: "+animalNames[faces[i]]+". Выбрать для "+names[i]);
            avatar.setOnClickListener(v->chooseAvatar(index)); r.addView(avatar,new LinearLayout.LayoutParams(dp(48),-1));
            EditText input=new EditText(this); input.setSingleLine(true); input.setText(names[i]); input.setTextSize(17); input.setTextColor(INK);
            input.setPadding(dp(4),0,dp(4),0); input.setBackgroundColor(Color.TRANSPARENT);
            input.setContentDescription("Имя игрока "+(i+1)); input.setSelectAllOnFocus(true);
            input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(18)});
            input.addTextChangedListener(new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){names[index]=s.toString();} public void afterTextChanged(android.text.Editable e){} });
            r.addView(input,new LinearLayout.LayoutParams(0,-1,1)); setup.grid.addView(r);
        }
        LinearLayout time=column(); TextView timeLabel=text("Время на ход",17,true); fitText(timeLabel,12,17);
        time.addView(timeLabel,new LinearLayout.LayoutParams(-1,dp(24))); LinearLayout times=row();
        for(int seconds:new int[]{30,60,90,120}) {
            TextView b=button(seconds+" с",seconds==duration,()->{duration=seconds; persist(); showSetup();});
            b.setPadding(dp(4),0,dp(4),0);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); lp.setMargins(dp(3),0,dp(3),0); times.addView(b,lp);
        }
        time.addView(times,new LinearLayout.LayoutParams(-1,dp(48))); setup.addView(time);
        Switch sounds=new Switch(this); sounds.setText("Звуковые сигналы"); sounds.setTextSize(16); sounds.setTextColor(INK); sounds.setChecked(sound);
        fitText(sounds,11,16); sounds.setOnCheckedChangeListener((b,on)->{sound=on; persist();}); setup.addView(sounds);
        LinearLayout footer=row(); TextView start=button("Начать игру  →",true,()->{
            android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(setup.getWindowToken(),0);
            for(int i=0;i<4;i++) {names[i]=names[i].trim(); if(names[i].isEmpty())names[i]="Игрок "+(i+1);}
            timer=new TurnTimer(count,duration); game=true; persist(); showGame();
        });
        LinearLayout.LayoutParams startLp=new LinearLayout.LayoutParams(0,-1,1); startLp.rightMargin=dp(8); footer.addView(start,startLp);
        TextView rules=button("ⓘ",false,()->showRules()); rules.setContentDescription("Краткие правила игры");
        footer.addView(rules,new LinearLayout.LayoutParams(dp(52),-1)); setup.addView(footer);
        setContentView(setup); setup.requestFocus(); setup.requestApplyInsets();
    }
    private class PlayerGrid extends ViewGroup {
        int columns=1;
        PlayerGrid() { super(MainActivity.this); }
        @Override protected void onMeasure(int widthSpec,int heightSpec) {
            int width=MeasureSpec.getSize(widthSpec),cell=(width-dp(6)*(columns-1))/columns;
            for(int i=0;i<getChildCount();i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cell,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(50),MeasureSpec.EXACTLY));
            setMeasuredDimension(width,((getChildCount()+columns-1)/columns)*dp(56));
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
            int cell=(getWidth()-dp(6)*(columns-1))/columns;
            for(int i=0;i<getChildCount();i++) { int x=(i%columns)*(cell+dp(6)),y=(i/columns)*dp(56); getChildAt(i).layout(x,y,x+cell,y+dp(50)); }
        }
    }
    private class SetupLayout extends ViewGroup {
        boolean keyboard; PlayerGrid grid; SetupLayoutPlan plan;
        SetupLayout() { super(MainActivity.this); }
        @Override protected void onMeasure(int widthSpec,int heightSpec) {
            int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);
            float d=getResources().getDisplayMetrics().density;
            float usableWidth=(width-getPaddingLeft()-getPaddingRight())/d;
            float usableHeight=(height-getPaddingTop()-getPaddingBottom())/d;
            boolean editing=keyboard || (Build.VERSION.SDK_INT<30 && usableHeight<360 && findFocus() instanceof EditText);
            plan=new SetupLayoutPlan(usableWidth,usableHeight,count,editing); grid.columns=plan.columns;
            for(int i=0;i<getChildCount();i++) {
                GameLayoutPlan.Box box=plan.boxes[i];
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(Math.max(0,Math.round(box.right*d)-Math.round(box.left*d)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(0,Math.round(box.bottom*d)-Math.round(box.top*d)),MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(width,height);
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
            float d=getResources().getDisplayMetrics().density;
            for(int i=0;i<getChildCount();i++) {
                GameLayoutPlan.Box box=plan.boxes[i];
                getChildAt(i).layout(getPaddingLeft()+Math.round(box.left*d),getPaddingTop()+Math.round(box.top*d),getPaddingLeft()+Math.round(box.right*d),getPaddingTop()+Math.round(box.bottom*d));
            }
        }
    }
    private void chooseAvatar(int player) {
        String[] options=new String[avatars.length]; for(int i=0;i<avatars.length;i++) options[i]=avatars[i]+"   "+animalNames[i];
        new AlertDialog.Builder(this).setTitle("Аватар · "+(names[player].isEmpty()?"Игрок "+(player+1):names[player]))
            .setSingleChoiceItems(options,faces[player],(d,which)->{ faces[player]=which; persist(); d.dismiss(); showSetup(); })
            .setNegativeButton("Отмена",null).show();
    }
    private void showGame() {
        game=true; baseGame(); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout header=row(); TextView logo=text("Румикуб",28,true); header.addView(logo,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView settings=button("⚙",false,()->confirmExit()); settings.setContentDescription("Настройки и новая игра"); header.addView(settings,new LinearLayout.LayoutParams(dp(52),dp(48))); addGame(header);
        playerStrip=row(); addGame(playerStrip);
        round=text("",13,true); round.setGravity(Gravity.CENTER); addGame(round);
        headline=text("",27,true); headline.setGravity(Gravity.CENTER); headline.setSingleLine(true); fitText(headline,14,27); addGame(headline);
        tile=new TimerTile(); tile.setOnClickListener(v->{
            if(timer.state==TurnTimer.State.PAUSED) { timer.resume(SystemClock.elapsedRealtime()); }
            else timer.startOrNext(SystemClock.elapsedRealtime());
            previousSecond=duration; beep(ToneGenerator.TONE_PROP_ACK,100); persist(); refreshGame();
        }); tile.setFocusable(true);
        addGame(tile);
        hint=text("",13,false); hint.setGravity(Gravity.CENTER); fitText(hint,10,13); addGame(hint);
        next=text("",18,true); next.setPadding(dp(16),0,dp(12),0); next.setBackground(bg(0xfffff9f0,16,true)); next.setSingleLine(true); fitText(next,12,18); addGame(next);
        LinearLayout controls=row(); pauseButton=button("Пауза",false,()->{
            if(timer.state==TurnTimer.State.PAUSED)timer.resume(SystemClock.elapsedRealtime()); else timer.pause(SystemClock.elapsedRealtime());
            persist(); refreshGame();
        });
        TextView audio=button(sound?"Звук вкл.":"Звук выкл.",false,()->{});
        audio.setOnClickListener(v->{sound=!sound; audio.setText(sound?"Звук вкл.":"Звук выкл."); persist(); if(sound)beep(ToneGenerator.TONE_PROP_BEEP,100);});
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(52),1); cp.setMargins(0,0,dp(8),0); controls.addView(pauseButton,cp); controls.addView(audio,new LinearLayout.LayoutParams(0,dp(52),1)); addGame(controls);
        refreshGame(); handler.removeCallbacks(ticker); handler.post(ticker);
    }
    private void refreshGame() {
        playerStrip.removeAllViews();
        for(int i=0;i<count;i++) {
            TextView p=text(avatars[faces[i]]+"\n"+names[i],15,true); p.setGravity(Gravity.CENTER); p.setMaxLines(2); fitText(p,10,15); p.setEllipsize(android.text.TextUtils.TruncateAt.END);
            p.setBackground(bg(i==timer.player?BLUE:0xfffff9f0,16,i!=timer.player)); p.setTextColor(i==timer.player?Color.WHITE:INK);
            p.setContentDescription(names[i]+(i==timer.player?", текущий игрок":""));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); lp.setMargins(dp(3),0,dp(3),0); playerStrip.addView(p,lp);
        }
        round.setText("ХОД "+timer.turn+"  ·  КРУГ "+((timer.turn-1)/count+1));
        headline.setText((timer.state==TurnTimer.State.PAUSED?"Пауза · ":"Ходит ")+names[timer.player]);
        next.setText(avatars[faces[(timer.player+1)%count]]+"   Далее · "+names[(timer.player+1)%count]);
        hint.setText(timer.state==TurnTimer.State.READY?"Первый игрок запускает таймер":timer.state==TurnTimer.State.PAUSED?"Таймер остановлен":timer.state==TurnTimer.State.EXPIRED?"Время вышло. Нажмите, чтобы передать ход":"Нажмите, чтобы передать ход");
        pauseButton.setText(timer.state==TurnTimer.State.PAUSED?"Продолжить":"Пауза");
        pauseButton.setEnabled(timer.state==TurnTimer.State.RUNNING || timer.state==TurnTimer.State.PAUSED); pauseButton.setAlpha(pauseButton.isEnabled()?1f:.45f);
        tile.setContentDescription(timer.state==TurnTimer.State.READY?"Старт таймера":timer.state==TurnTimer.State.PAUSED?"Продолжить таймер":"Ход завершён. Передать ход следующему игроку"); tile.invalidate();
    }
    private void confirmExit() {
        timer.pause(SystemClock.elapsedRealtime()); refreshGame(); persist();
        new AlertDialog.Builder(this).setTitle("Завершить текущую игру?").setMessage("Имена, аватары и время сохранятся. Новая игра начнётся с первого игрока.")
            .setPositiveButton("К настройкам",(d,w)->{game=false; persist(); showSetup();})
            .setNegativeButton("Остаться",null).show();
    }
    @Override public void onBackPressed() { if(game)confirmExit(); else super.onBackPressed(); }
    private void beep(int tone,int length) { if(sound && tones!=null) try { tones.startTone(tone,length); } catch(RuntimeException ignored){} }
    private void persist() {
        android.content.SharedPreferences.Editor p=getSharedPreferences("game",MODE_PRIVATE).edit();
        p.putInt("count",count).putInt("duration",duration).putBoolean("sound",sound).putBoolean("game",game);
        for(int i=0;i<4;i++)p.putString("name"+i,names[i]).putInt("face"+i,faces[i]);
        if(timer!=null && game)p.putInt("player",timer.player).putInt("turn",timer.turn).putString("state",timer.state.name()).putLong("remaining",timer.remaining(SystemClock.elapsedRealtime()));
        p.apply();
    }
    @Override protected void onPause() {
        super.onPause(); if(game && timer!=null) { timer.pause(SystemClock.elapsedRealtime()); if(tones!=null)tones.stopTone(); } persist(); handler.removeCallbacks(ticker);
    }
    @Override protected void onResume() { super.onResume(); if(game && tile!=null) { refreshGame(); handler.removeCallbacks(ticker); handler.post(ticker); } }
    @Override protected void onDestroy() { handler.removeCallbacks(ticker); if(tones!=null)tones.release(); super.onDestroy(); }
    private void showRules() {
        new AlertDialog.Builder(this).setTitle("Румикуб · памятка").setMessage("Цель — первым выложить все свои плитки.\n\nКаждый начинает с 14 плиток. Группа — 3–4 одинаковых числа разных цветов. Ряд — от 3 последовательных чисел одного цвета.\n\nПервая выкладка — минимум 30 очков со своей подставки. Затем можно дополнять и перестраивать комбинации на столе. Если хода нет, возьмите плитку и передайте ход по часовой стрелке.\n\nВ изученной редакции правил на ход даётся 60 секунд. Для вашей компании здесь доступны 30, 60, 90 и 120 секунд. Таймер не назначает штрафы автоматически.\n\nИсточник: rummikub.com, правила 2600-English.pdf. Это независимый таймер для настольной игры.")
            .setPositiveButton("Понятно",null).show();
    }
    private class TimerTile extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TimerTile() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
        private void label(Canvas c,String s,float y,float size,int color,boolean bold) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(size);
            paint.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); c.drawText(s,getWidth()/2f,y,paint);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w=getWidth(), h=getHeight(), unit=Math.min(w,h), cx=w/2, cy=h/2;
            paint.setStyle(Paint.Style.FILL); paint.setColor(0xfffff6e5); paint.setShadowLayer(dp(5),0,dp(3),0x30351d00);
            c.drawRoundRect(dp(6),dp(5),w-dp(6),h-dp(8),dp(30),dp(30),paint); paint.clearShadowLayer();
            float r=unit*.39f; RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(9)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setColor(0xffd9dbe4); c.drawOval(oval,paint);
            int seconds=timer.displayedSeconds(SystemClock.elapsedRealtime()); int accent=seconds<=10 && timer.state!=TurnTimer.State.READY?RED:BLUE;
            paint.setColor(accent); c.drawArc(oval,-90,360f*timer.remaining(SystemClock.elapsedRealtime())/(duration*1000f),false,paint);
            String time=String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
            label(c,time,cy+unit*.035f,unit*.205f,INK,true);
            String action=timer.state==TurnTimer.State.READY?"Старт  ▶":timer.state==TurnTimer.State.PAUSED?"Продолжить  ▶":timer.state==TurnTimer.State.EXPIRED?"Следующий игрок  →":"Ход завершён  →";
            label(c,action,cy+unit*.19f,unit*.052f,accent,true);
        }
    }
}
