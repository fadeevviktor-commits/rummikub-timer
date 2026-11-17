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
    private final int INK = Color.rgb(25,43,72), BLUE = Color.rgb(34,91,206), CREAM = Color.rgb(248,242,232), RED = Color.rgb(202,53,48);
    private final String[] avatars = {"🦊","🐻","🐱","🐼","🐶","🐨","🐯","🦁","🐸","🐰","🐟","🐏","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓"};
    private final String[] animalNames = {"Лиса","Медведь","Кот","Панда","Собака","Коала","Тигр","Лев","Лягушка","Кролик","Рыбка","Барашек","Овен","Телец","Близнецы","Рак","Лев · Зодиак","Дева","Весы","Скорпион","Стрелец","Козерог","Водолей","Рыбы"};
    private String[] names = {"Анна","Миша","Катя","Саша"};
    private int[] faces = {0,1,2,3};
    private int count = 4, duration = 60, firstPlayer = 0, previousSecond = -1;
    private final Bitmap[] avatarBitmaps=new Bitmap[24];
    private boolean sound = true, game = false;
    private LinearLayout identity;
    private ImageView currentAvatar;
    private TextView currentStatus;
    private boolean settingsMigrated;
    private GameLayout gameLayout;
    private TextView headline, round, next, pauseButton;
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
            if (expired) { refreshGame(); persist(); Toast.makeText(MainActivity.this,"Время вышло. Ход: "+displayName(timer.player),Toast.LENGTH_SHORT).show(); }
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
        firstPlayer=Math.max(0,Math.min(count-1,p.getInt("firstPlayer",0)));
        for (int i=0;i<4;i++) { names[i]=p.getString("name"+i,names[i]); faces[i]=Math.max(0,Math.min(avatars.length-1,p.getInt("face"+i,i))); }
        settingsMigrated=p.getBoolean("orderedListV2",false);
        game=p.getBoolean("game",false);
        if(!settingsMigrated && !game) migrateOrder();
        if (game) {
            timer=new TurnTimer(count,duration);
            TurnTimer.State state;
            try { state=TurnTimer.State.valueOf(p.getString("state","PAUSED")); } catch (Exception e) { state=TurnTimer.State.PAUSED; }
            timer.restore(p.getInt("player",0),p.getInt("turn",1),state,p.getLong("remaining",duration*1000L));
            showGame();
        } else showSetup();
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int color,int radius,boolean border) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(border)d.setStroke(dp(1),0xffe4ded2); return d;
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
        fitText(v,14,16); v.setElevation(0); v.setOnClickListener(w->action.run()); v.setFocusable(true); return v;
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
            if(currentAvatar!=null && boxes.length>2)currentAvatar.setVisibility(boxes[2].bottom-boxes[2].top<90?View.GONE:View.VISIBLE);
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
    private void migrateOrder() {
        String[] oldNames=names.clone(); int[] oldFaces=faces.clone();
        for(int i=0;i<count;i++){int source=(firstPlayer+i)%count;names[i]=oldNames[source];faces[i]=oldFaces[source];}
        firstPlayer=0; settingsMigrated=true;
    }
    private void hideKeyboard(View view) {
        ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(),0);
        view.clearFocus();
    }
    private void showSetup() {
        handler.removeCallbacks(ticker); game=false;
        if(!settingsMigrated)migrateOrder(); firstPlayer=0;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        SetupLayout setup=new SetupLayout(); setup.setBackgroundColor(CREAM); setup.setFocusableInTouchMode(true);
        setup.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());
                setup.keyboard=insets.isVisible(WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom)); v.requestLayout();
            } return insets;
        });
        LinearLayout header=column(); LinearLayout top=row();
        top.addView(text("Руммикуб",23,true),new LinearLayout.LayoutParams(0,dp(48),1));
        TextView menu=menuButton(()->showMenu(false)); top.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48))); header.addView(top);
        TextView title=text("Собираемся играть",26,true); fitText(title,20,26); header.addView(title,new LinearLayout.LayoutParams(-1,dp(40)));
        header.addView(text("Игроки идут по порядку списка",13,false),new LinearLayout.LayoutParams(-1,dp(28))); setup.addSection(header);
        LinearLayout choices=row(); choices.addView(text("Игроки",16,true),new LinearLayout.LayoutParams(0,-1,1));
        for(int n=2;n<=4;n++) {
            final int num=n; TextView choice=button(""+n,n==count,()->{hideKeyboard(setup);count=num;persist();showSetup();});
            choice.setPadding(0,0,0,0); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(44),dp(44));lp.leftMargin=dp(4);choices.addView(choice,lp);
        } setup.addSection(choices);
        setup.grid=new PlayerGrid(); setup.addSection(setup.grid);
        for(int i=0;i<count;i++) {
            final int index=i; LinearLayout player=row();
            TextView number=text(""+(i+1),12,false); number.setTextColor(0xff6d736f);player.addView(number,new LinearLayout.LayoutParams(dp(18),-1));
            ImageView avatar=avatarIcon(faces[i]); avatar.setContentDescription("Выбрать аватар: "+displayName(i)+". "+animalNames[faces[i]]);avatar.setOnClickListener(v->{hideKeyboard(setup);chooseAvatar(index);});player.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout info=column(); EditText input=new EditText(this);input.setSingleLine(true);input.setText(names[i]);input.setTextSize(17);input.setTextColor(INK);input.setPadding(dp(8),0,0,0);input.setBackgroundColor(Color.TRANSPARENT);input.setSelectAllOnFocus(true);
            input.setContentDescription("Имя игрока "+(i+1));input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(18)});
            input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int c,int f){} public void onTextChanged(CharSequence t,int st,int before,int c){names[index]=t.toString();} public void afterTextChanged(android.text.Editable e){}});
            input.setOnFocusChangeListener((v,focused)->{if(!focused)persist();});
            input.setOnEditorActionListener((v,action,event)->{hideKeyboard(setup);setup.requestFocus();persist();return true;});
            info.addView(input,new LinearLayout.LayoutParams(-1,i==0?dp(34):dp(54)));
            if(i==0){TextView first=text("Начинает игру",12,false);first.setTextColor(BLUE);first.setPadding(dp(8),0,0,0);info.addView(first,new LinearLayout.LayoutParams(-1,dp(20)));}
            player.addView(info,new LinearLayout.LayoutParams(0,dp(54),1));
            TextView handle=text("≡",25,false);handle.setGravity(Gravity.CENTER);handle.setTextColor(0xff6d736f);handle.setFocusable(true);handle.setContentDescription("Перетащить "+displayName(i)+". Нажмите для выбора позиции");
            handle.setOnClickListener(v->choosePosition(index));
            float[] start=new float[2]; boolean[] dragging={false};
            handle.setOnTouchListener((v,event)->{
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN){start[0]=event.getX();start[1]=event.getY();dragging[0]=false;v.getParent().requestDisallowInterceptTouchEvent(true);return true;}
                if(event.getActionMasked()==MotionEvent.ACTION_MOVE && !dragging[0] && Math.hypot(event.getX()-start[0],event.getY()-start[1])>dp(6)){
                    hideKeyboard(setup);dragging[0]=v.startDragAndDrop(ClipData.newPlainText("player",""+index),new View.DragShadowBuilder(player),Integer.valueOf(index),0);return true;
                }
                if(event.getActionMasked()==MotionEvent.ACTION_UP){v.getParent().requestDisallowInterceptTouchEvent(false);if(!dragging[0])v.performClick();return true;}
                if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){v.getParent().requestDisallowInterceptTouchEvent(false);return true;}return true;
            });
            player.addView(handle,new LinearLayout.LayoutParams(dp(48),dp(54)));
            player.setOnDragListener((v,event)->{
                if(!(event.getLocalState() instanceof Integer))return false;
                switch(event.getAction()){
                    case DragEvent.ACTION_DRAG_ENTERED:v.setBackground(bg(0xffe5ecfa,12,false));break;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        int[] loc=new int[2],origin=new int[2];v.getLocationOnScreen(loc);setup.scroll.getLocationOnScreen(origin);
                        float y=loc[1]-origin[1]+event.getY();if(y<dp(60))setup.scroll.smoothScrollBy(0,-dp(24));else if(y>setup.scroll.getHeight()-dp(60))setup.scroll.smoothScrollBy(0,dp(24));break;
                    case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setBackgroundColor(Color.TRANSPARENT);break;
                    case DragEvent.ACTION_DROP:
                        int from=(Integer)event.getLocalState();PlayerOrder.move(names,faces,count,from,index);firstPlayer=0;persist();showSetup();break;
                }return true;
            });
            setup.grid.addView(player);
        }
        LinearLayout time=column();time.setPadding(0,dp(16),0,0);time.addView(text("Время на ход",16,true),new LinearLayout.LayoutParams(-1,dp(36)));LinearLayout times=row();
        for(int seconds:new int[]{30,60,90,120}) {
            TextView choice=button(seconds+" с",seconds==duration,()->{hideKeyboard(setup);duration=seconds;persist();showSetup();});choice.setPadding(dp(2),0,dp(2),0);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(seconds!=120)lp.rightMargin=dp(6);times.addView(choice,lp);
        }time.addView(times);setup.addSection(time);
        TextView start=button("Начать игру  →",true,()->{
            hideKeyboard(setup);for(int i=0;i<4;i++){names[i]=names[i].trim();if(names[i].isEmpty())names[i]="Игрок "+(i+1);}
            firstPlayer=0;timer=new TurnTimer(count,duration,0);game=true;persist();showGame();
        });setup.setFooter(start);setContentView(setup);setup.requestFocus();setup.requestApplyInsets();
    }
    private void choosePosition(int from) {
        String[] positions=new String[count];for(int i=0;i<count;i++)positions[i]=(i+1)+". "+displayName(i)+(i==0?" · начинает игру":"");
        new AlertDialog.Builder(this).setTitle("Переместить: "+displayName(from)).setItems(positions,(d,to)->{PlayerOrder.move(names,faces,count,from,to);firstPlayer=0;persist();showSetup();}).setNegativeButton("Отмена",null).show();
    }
    private class PlayerGrid extends ViewGroup {
        PlayerGrid(){super(MainActivity.this);}
        @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);for(int i=0;i<getChildCount();i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(68),MeasureSpec.EXACTLY));setMeasuredDimension(width,getChildCount()*dp(68));}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount();i++)getChildAt(i).layout(0,i*dp(68),getWidth(),(i+1)*dp(68));}
        @Override protected void dispatchDraw(Canvas c){super.dispatchDraw(c);Paint line=new Paint();line.setColor(0xffe4ded2);line.setStrokeWidth(dp(1));for(int i=1;i<=getChildCount();i++)c.drawLine(0,i*dp(68)-1,getWidth(),i*dp(68)-1,line);}
    }
    private class SetupLayout extends ViewGroup {
        boolean keyboard;PlayerGrid grid;SetupLayoutPlan plan;final ScrollView scroll;final SetupForm form;View footer;
        SetupLayout(){super(MainActivity.this);scroll=new ScrollView(MainActivity.this);form=new SetupForm();scroll.addView(form);addView(scroll);}
        void addSection(View v){form.addView(v);}void setFooter(View v){footer=v;addView(v);}
        @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w),height=MeasureSpec.getSize(h);float d=getResources().getDisplayMetrics().density;int usableWidth=width-getPaddingLeft()-getPaddingRight(),usableHeight=height-getPaddingTop()-getPaddingBottom();boolean editing=keyboard||(Build.VERSION.SDK_INT<30&&usableHeight/d<360&&findFocus() instanceof EditText);plan=new SetupLayoutPlan(usableWidth/d,usableHeight/d,count,editing);scroll.measure(MeasureSpec.makeMeasureSpec(usableWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(plan.viewport*d),MeasureSpec.EXACTLY));footer.measure(MeasureSpec.makeMeasureSpec(Math.round((plan.footer.right-plan.footer.left)*d),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(56),MeasureSpec.EXACTLY));setMeasuredDimension(width,height);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){float d=getResources().getDisplayMetrics().density;scroll.layout(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getPaddingTop()+Math.round(plan.viewport*d));GameLayoutPlan.Box box=plan.footer;footer.layout(getPaddingLeft()+Math.round(box.left*d),getPaddingTop()+Math.round(box.top*d),getPaddingLeft()+Math.round(box.right*d),getPaddingTop()+Math.round(box.bottom*d));}
        class SetupForm extends ViewGroup {
            SetupForm(){super(MainActivity.this);}
            @Override protected void onMeasure(int w,int h){float d=getResources().getDisplayMetrics().density;for(int i=0;i<getChildCount();i++){GameLayoutPlan.Box b=plan.boxes[i];getChildAt(i).measure(MeasureSpec.makeMeasureSpec(Math.round((b.right-b.left)*d),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round((b.bottom-b.top)*d),MeasureSpec.EXACTLY));}setMeasuredDimension(MeasureSpec.getSize(w),Math.round(plan.bodyHeight*d));}
            @Override protected void onLayout(boolean changed,int l,int t,int r,int b){float d=getResources().getDisplayMetrics().density;for(int i=0;i<getChildCount();i++){GameLayoutPlan.Box box=plan.boxes[i];getChildAt(i).layout(Math.round(box.left*d),Math.round(box.top*d),Math.round(box.right*d),Math.round(box.bottom*d));}}
        }
    }
    private String displayName(int i) { return names[i].trim().isEmpty()?"Игрок "+(i+1):names[i]; }
    private Bitmap avatarBitmap(int index) {
        if(avatarBitmaps[index]==null) {
            int resource=getResources().getIdentifier(String.format(Locale.ROOT,"avatar_%02d",index),"drawable",getPackageName());
            avatarBitmaps[index]=BitmapFactory.decodeResource(getResources(),resource);
        }
        return avatarBitmaps[index];
    }
    private ImageView avatarIcon(int index) {
        ImageView image=new ImageView(this); image.setImageBitmap(avatarBitmap(index)); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription(animalNames[index]); return image;
    }
    private CharSequence avatarText(int index,String label,int size) {
        android.text.SpannableString s=new android.text.SpannableString("\uFFFC"+label);
        android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(getResources(),avatarBitmap(index)); d.setBounds(0,0,dp(size),dp(size));
        s.setSpan(new android.text.style.ImageSpan(d,android.text.style.ImageSpan.ALIGN_BOTTOM),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); return s;
    }
    private void chooseAvatar(int player) { new AvatarPicker(player).show(); }
    private class AvatarPicker extends Dialog {
        final int player; int selected,tab; final GridLayout grid; final TextView animals,zodiac;
        AvatarPicker(int player) {
            super(MainActivity.this,android.R.style.Theme_Material_Light_NoActionBar);
            this.player=player; selected=faces[player]; tab=selected>=12?1:0;
            LinearLayout root=column(); root.setPadding(dp(16),dp(12),dp(16),dp(12)); root.setBackgroundColor(CREAM);
            root.setOnApplyWindowInsetsListener((v,insets)->{ if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()); v.setPadding(dp(16)+bars.left,dp(12)+bars.top,dp(16)+bars.right,dp(12)+bars.bottom);}return insets;});
            LinearLayout head=row(); TextView title=text("Выберите аватар",25,true); fitText(title,18,25); head.addView(title,new LinearLayout.LayoutParams(0,dp(48),1)); head.addView(button("×",false,()->dismiss()),new LinearLayout.LayoutParams(dp(48),dp(48))); root.addView(head);
            LinearLayout tabs=row(); animals=button("Животные",false,()->{tab=0; render();}); zodiac=button("Зодиак",false,()->{tab=1; render();});
            tabs.addView(animals,new LinearLayout.LayoutParams(0,dp(48),1)); tabs.addView(zodiac,new LinearLayout.LayoutParams(0,dp(48),1));
            LinearLayout.LayoutParams tabsLp=new LinearLayout.LayoutParams(-1,dp(48)); tabsLp.topMargin=dp(8); tabsLp.bottomMargin=dp(10); root.addView(tabs,tabsLp);
            ScrollView scroll=new ScrollView(MainActivity.this); grid=new GridLayout(MainActivity.this); grid.setColumnCount(3); scroll.addView(grid,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            TextView apply=button("Выбрать",true,()->{faces[player]=selected; persist(); dismiss(); showSetup();}); LinearLayout.LayoutParams applyLp=new LinearLayout.LayoutParams(-1,dp(56)); applyLp.topMargin=dp(10); root.addView(apply,applyLp);
            setContentView(root); render();
        }
        void render() {
            animals.setTextColor(tab==0?BLUE:0xff6a6570); zodiac.setTextColor(tab==1?BLUE:0xff6a6570);
            for(int i=0;i<2;i++) {
                TextView label=i==0?animals:zodiac;
                android.graphics.drawable.LayerDrawable back=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{new android.graphics.drawable.ColorDrawable(CREAM),bg(i==tab?BLUE:0xffdcd1c2,2,false)});
                back.setLayerInset(1,0,dp(44),0,0); label.setBackground(back); label.setElevation(0);
            }
            grid.removeAllViews();
            float d=getResources().getDisplayMetrics().density;
            int width=(int)(getResources().getDisplayMetrics().widthPixels/d)-32;
            int height=(int)(getResources().getDisplayMetrics().heightPixels/d);
            int card=Math.max(88,Math.min(140,(height-250)/4));
            for(int j=0;j<12;j++) {
                final int index=tab*12+j; LinearLayout cell=column(); cell.setPadding(dp(3),dp(3),dp(3),dp(4)); cell.setGravity(Gravity.CENTER); cell.setElevation(dp(2));
                GradientDrawable back=bg(0xfffff9f0,15,true); if(selected==index)back.setStroke(dp(2),BLUE); cell.setBackground(back);
                ImageView icon=avatarIcon(index); cell.addView(icon,new LinearLayout.LayoutParams(-1,0,1));
                TextView label=text(animalNames[index].replace(" · Зодиак",""),14,true); fitText(label,10,14); label.setGravity(Gravity.CENTER); label.setSingleLine(true); cell.addView(label,new LinearLayout.LayoutParams(-1,dp(24)));
                FrameLayout cardView=new FrameLayout(MainActivity.this); cardView.addView(cell,new FrameLayout.LayoutParams(-1,-1));
                if(selected==index) {
                    TextView check=text("✓",14,true); check.setGravity(Gravity.CENTER); check.setTextColor(Color.WHITE); check.setBackground(bg(BLUE,20,false));
                    FrameLayout.LayoutParams checkLp=new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.TOP|Gravity.END); checkLp.setMargins(0,dp(5),dp(5),0); cardView.addView(check,checkLp);
                }
                cardView.setOnClickListener(v->{selected=index; render();}); cardView.setContentDescription(animalNames[index]+(selected==index?", выбран":"")); cardView.setFocusable(true); cardView.setSelected(selected==index);
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(j/3),GridLayout.spec(j%3,1f)); lp.width=0; lp.height=dp(card); lp.setMargins(dp(3),dp(3),dp(3),dp(3)); grid.addView(cardView,lp);
            }

        }
        @Override protected void onStart(){super.onStart(); getWindow().setLayout(-1,-1); getWindow().setStatusBarColor(CREAM); getWindow().setNavigationBarColor(CREAM); if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}
    }
    private TextView menuButton(Runnable action) {
        TextView v=text("···",27,true);v.setGravity(Gravity.CENTER);v.setContentDescription("Меню");v.setFocusable(true);v.setOnClickListener(w->action.run());return v;
    }
    private void showMenu(boolean playing) {
        if(playing){timer.pause(SystemClock.elapsedRealtime());persist();refreshGame();}
        String[] items=playing?new String[]{sound?"Выключить звук":"Включить звук","Памятка","Завершить игру"}:new String[]{sound?"Выключить звук":"Включить звук","Памятка"};
        new AlertDialog.Builder(this).setTitle("Меню").setItems(items,(d,item)->{
            if(item==0){sound=!sound;persist();if(sound)beep(ToneGenerator.TONE_PROP_BEEP,100);}
            else if(item==1)showRules();else confirmExit();
        }).setNegativeButton("Закрыть",null).show();
    }
    private void showGame() {
        game=true;baseGame();getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout header=row();header.addView(text("Руммикуб",23,true),new LinearLayout.LayoutParams(0,dp(48),1));header.addView(menuButton(()->showMenu(true)),new LinearLayout.LayoutParams(dp(48),dp(48)));addGame(header);
        round=text("",13,false);round.setTextColor(0xff6d736f);round.setGravity(Gravity.CENTER);addGame(round);
        identity=column();identity.setGravity(Gravity.CENTER);currentAvatar=avatarIcon(faces[timer.player]);identity.addView(currentAvatar,new LinearLayout.LayoutParams(dp(58),0,1));
        headline=text("",26,true);headline.setGravity(Gravity.CENTER);headline.setSingleLine(true);fitText(headline,18,26);identity.addView(headline,new LinearLayout.LayoutParams(-1,dp(32)));
        currentStatus=text("",13,false);currentStatus.setTextColor(0xff6d736f);currentStatus.setGravity(Gravity.CENTER);identity.addView(currentStatus,new LinearLayout.LayoutParams(-1,dp(20)));addGame(identity);
        tile=new TimerTile();tile.setFocusable(true);tile.setOnClickListener(v->{
            if(timer.state==TurnTimer.State.PAUSED)timer.resume(SystemClock.elapsedRealtime());else timer.startOrNext(SystemClock.elapsedRealtime());
            previousSecond=duration;beep(ToneGenerator.TONE_PROP_ACK,100);persist();refreshGame();
        });addGame(tile);
        next=text("",15,false);next.setGravity(Gravity.CENTER);next.setTextColor(0xff6d736f);next.setSingleLine(true);fitText(next,13,15);addGame(next);
        pauseButton=button("Пауза",false,()->{if(timer.state==TurnTimer.State.PAUSED)timer.resume(SystemClock.elapsedRealtime());else timer.pause(SystemClock.elapsedRealtime());persist();refreshGame();});addGame(pauseButton);
        refreshGame();handler.removeCallbacks(ticker);handler.post(ticker);
    }
    private void refreshGame() {
        boolean paused=timer.state==TurnTimer.State.PAUSED,ready=timer.state==TurnTimer.State.READY;
        currentAvatar.setImageBitmap(avatarBitmap(faces[timer.player]));currentAvatar.setContentDescription(animalNames[faces[timer.player]]);
        round.setText(paused?"ПАУЗА":"КРУГ "+((timer.turn-1)/count+1));headline.setText(displayName(timer.player));
        currentStatus.setText(paused?"Таймер остановлен":ready?"Начинает игру":"Ваш ход");
        next.setText(avatarText(faces[(timer.player+1)%count],"  Далее: "+displayName((timer.player+1)%count),28));
        pauseButton.setText(paused?"Продолжить  ▶":"Ⅱ  Пауза");pauseButton.setBackground(bg(paused?BLUE:CREAM,16,!paused));pauseButton.setTextColor(paused?Color.WHITE:INK);
        pauseButton.setEnabled(!ready);pauseButton.setAlpha(ready?.4f:1f);
        tile.setContentDescription(ready?"Старт таймера":paused?"Продолжить таймер":"Передать ход: "+displayName((timer.player+1)%count));tile.invalidate();
    }
    private void confirmExit() {
        timer.pause(SystemClock.elapsedRealtime());refreshGame();persist();
        new AlertDialog.Builder(this).setTitle("Завершить игру?").setMessage("Имена, аватары и время сохранятся. Новая игра начнётся с первого игрока в списке.")
            .setPositiveButton("Завершить",(d,w)->{game=false;persist();showSetup();}).setNegativeButton("Остаться",null).show();
    }
    @Override public void onBackPressed() { if(game)confirmExit(); else super.onBackPressed(); }
    private void beep(int tone,int length) { if(sound && tones!=null) try { tones.startTone(tone,length); } catch(RuntimeException ignored){} }
    private void persist() {
        android.content.SharedPreferences.Editor p=getSharedPreferences("game",MODE_PRIVATE).edit();
        p.putBoolean("orderedListV2",settingsMigrated);
        p.putInt("count",count).putInt("duration",duration).putBoolean("sound",sound).putBoolean("game",game).putInt("firstPlayer",firstPlayer);
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
        new AlertDialog.Builder(this).setTitle("Руммикуб · памятка").setMessage("Цель — первым выложить все свои плитки.\n\nКаждый начинает с 14 плиток. Группа — 3–4 одинаковых числа разных цветов. Ряд — от 3 последовательных чисел одного цвета.\n\nПервая выкладка — минимум 30 очков со своей подставки. Затем можно дополнять и перестраивать комбинации на столе. Если хода нет, возьмите плитку и передайте ход по часовой стрелке.\n\nВ изученной редакции правил на ход даётся 60 секунд. Для вашей компании здесь доступны 30, 60, 90 и 120 секунд. Таймер не назначает штрафы автоматически.\n\nИсточник: rummikub.com, правила 2600-English.pdf. Это независимый таймер для настольной игры.")
            .setPositiveButton("Понятно",null).show();
    }
    private class TimerTile extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TimerTile() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.Button");
            info.setText(displayName(timer.player)+", осталось "+timer.displayedSeconds(SystemClock.elapsedRealtime())+" секунд");
        }
        private void label(Canvas c,String s,float y,float size,int color,boolean bold) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(size);
            paint.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); c.drawText(s,getWidth()/2f,y,paint);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);float w=getWidth(),h=getHeight(),unit=Math.min(w,h),cx=w/2,cy=h/2;
            boolean paused=timer.state==TurnTimer.State.PAUSED,ready=timer.state==TurnTimer.State.READY;
            int seconds=timer.displayedSeconds(SystemClock.elapsedRealtime());int accent=paused?0xff909b9e:seconds<=10&&!ready?RED:BLUE;
            float radius=unit*.45f;RectF oval=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(4));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xffe4e1d9);c.drawOval(oval,paint);
            paint.setColor(accent);c.drawArc(oval,-90,360f*timer.remaining(SystemClock.elapsedRealtime())/(duration*1000f),false,paint);
            String time=String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
            label(c,time,cy+unit*.045f,unit*.24f,INK,true);
            String action=ready?"Старт таймера  ▶":paused?"Осталось на ход":"Передать ход  →";
            label(c,action,cy+unit*.18f,Math.max(dp(12),Math.min(dp(16),unit*.05f)),paused?0xff6d736f:accent,false);
        }
    }
}
