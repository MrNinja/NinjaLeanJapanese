package gdg.ninja.ui;

import gdg.nat.ninjalearnjapanese.R;
import gdg.ninja.database.DatabaseHandler;
import gdg.ninja.framework.BaseFragment;
import gdg.ninja.gameinfo.CategoriesInfo;
import gdg.ninja.gameinfo.QuestInfo;
import gdg.ninja.navigate.NavigationBar.BTN_LEFT_MODE;
import gdg.ninja.navigate.NavigationBar.BTN_RIGHT_MODE;
import gdg.ninja.navigate.NavigationBar.INavigationBarListener;
import gdg.ninja.util.App;
import gdg.ninja.util.ConfigPreference;
import gdg.ninja.util.QuestGenerator;
import gdg.ninja.util.ShareUtils;
import gdg.ninja.util.ShareUtils.SHARE_TYPE;
import gdg.ninja.util.SoundUtils;
import gdg.ninja.util.SoundUtils.SOUND_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.squareup.picasso.Picasso;

public class QuestFragment extends BaseFragment implements
		INavigationBarListener, OnClickListener {
	private static final String KEY_CATEGORY_ID = "category";
	private static final String KEY_QUEST_ID = "quest";
	private final int DELAY_TIME = 1 * 500;

	private ImageView mImgAvatar;
	private ImageView mImgShareFacebook;
	private ImageView mImgShareGoogle;
	private ImageView mImgBomb;
	private ImageView mImgFlag;

	private TextView mTxtBombCount;
	private TextView mTxtCompassCount;

	private LinearLayout mLnlAnswerLayoutOne;
	private LinearLayout mLnlAnswerLayoutTwo;
	private LinearLayout mLnlQuestLayoutOne;
	private LinearLayout mLnlQuestLayoutTwo;
	private LinearLayout mLnlQuestLayoutThree;
	private Tag[] mListAnswer;
	private Tag[] mListQuest;

	private int mCategoryId;
	private int mQuestId;
	private int sizeLineAnswerOne = 0;
	private int sizeLineAnswerTwo = 0;
	private int sizeLineQuestOne = 0;
	private int sizeLineQuestTwo = 0;
	private int sizeLineQuestThree = 0;
	private String mScreenTitle = "";
	private boolean isAnimate = false;
	private boolean isTextAnimate = false;
	private boolean isBombed = false;

	private int numAnswered = 0;
	private int numOfWrongAnswered = 0;

	private int numberOfBomb = 0;
	private int numberOfCompass = 0;

	private int numberOfCompassUsed = 0;

	public static QuestFragment getInstance(int questId, int cateId) {
		QuestFragment fragment = new QuestFragment();
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_CATEGORY_ID, cateId);
		bundle.putInt(KEY_QUEST_ID, questId);
		fragment.setArguments(bundle);
		return fragment;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mCategoryId = savedInstanceState.getInt(KEY_CATEGORY_ID);
			mQuestId = savedInstanceState.getInt(KEY_QUEST_ID);
		} else {
			Bundle bundle = getArguments();
			mCategoryId = bundle.getInt(KEY_CATEGORY_ID);
			mQuestId = bundle.getInt(KEY_QUEST_ID);
		}
		numberOfBomb = ConfigPreference.getInstance().getNumberOfBomb();
		numberOfCompass = ConfigPreference.getInstance().getNumberOfCompass();
		mScreenTitle = App.getCategoryById(mCategoryId).getCateName();

		// Display help when user play first time
		if (ConfigPreference.getInstance().isFirstTime()) {
			Handler mHandler = new Handler();
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					Intent intent = new Intent(getActivity(),
							HelpActivity.class);
					startActivity(intent);

				}
			}, 2000);
			ConfigPreference.getInstance().saveIsFirstTime();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fg_quest, container, false);
		initView(view);
		return view;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// FacebookUtil.onActivityResult(getActivity(), requestCode, resultCode,
		// data);
	}

	public void initView(View v) {
		// Declaring view
		mImgAvatar = (ImageView) v.findViewById(R.id.img_avatar);
		mImgShareFacebook = (ImageView) v.findViewById(R.id.btn_share_facebook);
		mImgShareGoogle = (ImageView) v.findViewById(R.id.btn_share_google);
		mImgBomb = (ImageView) v.findViewById(R.id.btn_boom);
		mImgFlag = (ImageView) v.findViewById(R.id.btn_flag);
		mTxtBombCount = (TextView) v.findViewById(R.id.rate_one);
		mTxtCompassCount = (TextView) v.findViewById(R.id.rate_two);
		mImgShareFacebook.setOnClickListener(this);
		mImgShareGoogle.setOnClickListener(this);
		mImgBomb.setOnClickListener(this);
		mImgFlag.setOnClickListener(this);

		// Declaring group view
		mLnlAnswerLayoutOne = (LinearLayout) v.findViewById(R.id.ltv_list_one);
		mLnlAnswerLayoutTwo = (LinearLayout) v.findViewById(R.id.ltv_list_two);
		mLnlQuestLayoutOne = (LinearLayout) v.findViewById(R.id.quest_one);
		mLnlQuestLayoutTwo = (LinearLayout) v.findViewById(R.id.quest_two);
		mLnlQuestLayoutThree = (LinearLayout) v.findViewById(R.id.quest_three);

		// Set bomb count and compass count
		mTxtBombCount.setText("" + numberOfBomb);
		mTxtCompassCount.setText("" + numberOfCompass);

		// Get quest info
		QuestInfo questInfo = App.getQuestById(mQuestId, mCategoryId);

		// Set quest image
		Picasso.with(getActivity())
				.load(questInfo.getImgPath().replace("assets",
						"file:///android_asset"))
				.placeholder(R.drawable.dummy_image).into(mImgAvatar);

		QuestGenerator questGenerator = new QuestGenerator(getActivity());
		// Answer generating
		List<String> answer = questGenerator.getAnswer(questInfo.getAnswer());
		int size = answer.size();
		mListAnswer = new Tag[size];
		final int MAX_TAG_PER_ROWS = getResources().getInteger(
				R.integer.max_tag_per_row);
		if (size <= MAX_TAG_PER_ROWS) {
			sizeLineAnswerOne = size;
			sizeLineAnswerTwo = 0;
			for (int i = 0; i < size; i++) {
				setAnswerTag(i, answer.get(i), 1);
			}
			mLnlAnswerLayoutTwo.setVisibility(View.GONE);
		} else if (size <= 2 * MAX_TAG_PER_ROWS) {
			sizeLineAnswerOne = size / 2;
			sizeLineAnswerTwo = size - sizeLineAnswerOne;
			for (int i = 0; i < sizeLineAnswerOne; i++) {
				setAnswerTag(i, answer.get(i), 1);
			}
			for (int i = 0; i < sizeLineAnswerTwo; i++) {
				int index = sizeLineAnswerOne + i;
				setAnswerTag(index, answer.get(index), 2);
			}
		} else {
			mNaviManager.goBack();
		}
		numAnswered = 0;
		numOfWrongAnswered = 0;

		// Quest generating
		List<String> quest = questGenerator.getQuest(questInfo.getAnswer(),
				mCategoryId);
		size = quest.size();
		mListQuest = new Tag[size];
		if (size > 3 * MAX_TAG_PER_ROWS)
			mNaviManager.goBack();
		if (size <= MAX_TAG_PER_ROWS) {
			sizeLineQuestOne = size;
			sizeLineQuestTwo = 0;
			sizeLineQuestThree = 0;
			for (int i = 0; i < size; i++) {
				setQuestTag(i, quest.get(i), 1);
			}
			mLnlQuestLayoutTwo.setVisibility(View.GONE);
			mLnlQuestLayoutThree.setVisibility(View.GONE);
		} else if (size <= 2 * MAX_TAG_PER_ROWS) {
			sizeLineQuestOne = size / 2;
			sizeLineQuestTwo = size - sizeLineQuestOne;
			sizeLineQuestThree = 0;
			for (int i = 0; i < sizeLineQuestOne; i++) {
				setQuestTag(i, quest.get(i), 1);
			}
			for (int i = 0; i < sizeLineQuestTwo; i++) {
				int index = sizeLineQuestOne + i;
				setQuestTag(index, quest.get(index), 2);
			}
			mLnlQuestLayoutThree.setVisibility(View.GONE);
		} else if (size <= 3 * MAX_TAG_PER_ROWS) {
			sizeLineQuestOne = size / 3;
			sizeLineQuestTwo = (size - sizeLineQuestOne) / 2;
			sizeLineQuestThree = size - sizeLineQuestOne - sizeLineQuestTwo;
			for (int i = 0; i < sizeLineQuestOne; i++) {
				setQuestTag(i, quest.get(i), 1);
			}
			for (int i = 0; i < sizeLineQuestTwo; i++) {
				int index = sizeLineQuestOne + i;
				setQuestTag(index, quest.get(index), 2);
			}
			for (int i = 0; i < sizeLineQuestThree; i++) {
				int index = sizeLineQuestOne + sizeLineQuestTwo + i;
				setQuestTag(index, quest.get(index), 3);
			}
		} else {
			mNaviManager.goBack();
		}
	}

	private void setAnswerTag(int index, String answer, int viewLine) {
		mListAnswer[index] = new Tag();
		mListAnswer[index].string = answer;
		View view = View.inflate(getActivity(), R.layout.tag_answer, null);
		mListAnswer[index].view = (TextView) view.findViewById(R.id.answer_tag);
		if (viewLine == 1) {
			mLnlAnswerLayoutOne.addView(view);
		} else {
			mLnlAnswerLayoutTwo.addView(view);
		}
		mListAnswer[index].view.setText(mListAnswer[index].string);
		mListAnswer[index].view.setTag(index);
		mListAnswer[index].view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isAnimate)
					return;
				if (v.getTag() instanceof Integer) {
					int answerSize = mListAnswer.length;
					if (numAnswered > answerSize)
						return;
					int index = 0;
					try {
						index = Integer.parseInt(v.getTag().toString());
					} catch (NumberFormatException ex) {
						return;
					}
					if (mListAnswer[index].preView == null)
						return;
					mListAnswer[index].view.setText("");
					mListAnswer[index].view.setVisibility(View.INVISIBLE);
					mListAnswer[index].preView.setVisibility(View.VISIBLE);
					mListAnswer[index].preView = null;
					numAnswered--;
					SoundUtils.getInstance().play(SOUND_NAME.TYPING);
				}

			}
		});
	}

	private void setQuestTag(int index, String quest, int viewLine) {
		mListQuest[index] = new Tag();
		mListQuest[index].string = quest;
		View view = View.inflate(getActivity(), R.layout.tag_quest, null);
		mListQuest[index].view = (TextView) view.findViewById(R.id.quest_tag);
		if (viewLine == 1) {
			mLnlQuestLayoutOne.addView(view);
		} else if (viewLine == 2) {
			mLnlQuestLayoutTwo.addView(view);
		} else {
			mLnlQuestLayoutThree.addView(view);
		}
		mListQuest[index].view.setText(mListQuest[index].string);
		mListQuest[index].view.setTag(index);
		mListQuest[index].view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isAnimate)
					return;
				int answerSize = mListAnswer.length;
				numAnswered++;
				if (numAnswered > answerSize)
					return;
				if (numAnswered == answerSize) {
					String questText = ((TextView) v).getText().toString();
					mListAnswer[answerSize - 1].view.setText(questText);
					mListAnswer[answerSize - 1].view
							.setVisibility(View.VISIBLE);
					mListAnswer[answerSize - 1].preView = v;
					v.setVisibility(View.INVISIBLE);
					for (Tag tag : mListAnswer) {
						if (!tag.string.equals(tag.view.getText().toString())) {
							onWrongAnswer();
							return;
						}
					}
					onWinGame();
				} else {
					if (v instanceof TextView) {
						for (Tag tag : mListAnswer) {
							if (tag.preView == null) {
								String questText = ((TextView) v).getText()
										.toString();
								tag.view.setText(questText);
								tag.view.setVisibility(View.VISIBLE);
								tag.preView = v;
								v.setVisibility(View.INVISIBLE);
								SoundUtils.getInstance()
										.play(SOUND_NAME.TYPING);
								break;
							}
						}
					}
				}
			}
		});
	}

	/* When the last answer check wrong. Notice for user and remove this view */
	private void onWrongAnswer() {
		isAnimate = true;
		SoundUtils.getInstance().play(SOUND_NAME.WRONG);
		Animation animation = AnimationUtils.loadAnimation(getActivity(),
				R.anim.shake);
		mLnlAnswerLayoutOne.startAnimation(animation);
		mLnlAnswerLayoutTwo.startAnimation(animation);
		Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int lastIndex = mListAnswer.length - 1;
				mListAnswer[lastIndex].view.setText("");
				mListAnswer[lastIndex].view.setVisibility(View.INVISIBLE);
				mListAnswer[lastIndex].preView.setVisibility(View.VISIBLE);
				mListAnswer[lastIndex].preView = null;
				numAnswered--;
				isAnimate = false;
			}
		};
		handler.postDelayed(runnable, DELAY_TIME);

		// Increase number of wrong answered
		numOfWrongAnswered++;
	}

	private void onWinGame() {
		SoundUtils.getInstance().play(SOUND_NAME.RIGHT);
		// Calculate score base on wrong answered
		final int score;
		final boolean isBonus;
		if (numOfWrongAnswered > 10)
			score = 3;
		else if (numOfWrongAnswered > 5)
			score = 4;
		else if (numOfWrongAnswered > 1)
			score = 5;
		else
			score = 6;

		QuestInfo quest = App.getQuestById(mQuestId, mCategoryId);
		int oldScore = quest.getQuestStt();
		// Bonus bomb and compass for the first time win game
		if (oldScore == 0 && quest.getQuestId() % 3 == 0) {
			isBonus = true;
			numberOfCompass += 1;
			setNewBombAndCompass(++numberOfBomb, numberOfCompass);
		} else
			isBonus = false;

		// Only update score if new score is higher
		if (score > oldScore) {
			// Update Stt for current QuestList and CategoryList
			quest.setQuestStt(score);
			CategoriesInfo categoriesInfo = App.getCategoryById(mCategoryId);
			categoriesInfo.reCalculateStt();

			// Update score of current Quest and Stt of current Category in
			// database
			DatabaseHandler db = new DatabaseHandler(getActivity());
			db.updateScoreByQuestId(mQuestId, score);
			db.updateCateSttByCategoryId(mCategoryId, categoriesInfo.getStt());
		}

		dialog = new AlertDialog(getActivity(), R.style.full_screen_dialog) {
			@Override
			protected void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				setContentView(getViewForWinDialog(score, isBonus));
			}

			@Override
			public boolean onKeyDown(int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.dismiss();
					mNaviManager.goBack();
				}
				return super.onKeyDown(keyCode, event);
			}
		};
		dialog.show();
	}

	private View getViewForWinDialog(int score, boolean isBonus) {
		final QuestInfo questInfo = App.getQuestById(mQuestId, mCategoryId);

		final View winDialogView = View.inflate(getActivity(),
				R.layout.popup_finish, null);
		ImageView imgAvatar = (ImageView) winDialogView
				.findViewById(R.id.img_avatar);
		Picasso.with(getActivity())
				.load(questInfo.getImgPath().replace("assets",
						"file:///android_asset")).resize(250, 250)
				.placeholder(R.drawable.dummy_image).into(imgAvatar);

		TextView mTxtShowPoint = (TextView) winDialogView
				.findViewById(R.id.txt_welcome);
		Resources resources = App.getContext().getResources();
		mTxtShowPoint.setText(resources.getString(R.string.txt_you_got_point)
				+ " " + score + " " + resources.getString(R.string.txt_point));

		final TextSwitcher mTxtAnswer = (TextSwitcher) winDialogView
				.findViewById(R.id.txt_title_navi);
		mTxtAnswer.setFactory(new ViewFactory() {

			@Override
			public View makeView() {
				TextView t = new TextView(getActivity());
				t.setGravity(Gravity.CENTER_HORIZONTAL);
				t.setBackgroundColor(App.getContext().getResources()
						.getColor(R.color.answer_color));
				t.setTextColor(Color.WHITE);
				t.setTextSize(26);
				return t;
			}
		});
		mTxtAnswer.setText(questInfo.getAnswer());

		Animation in = AnimationUtils.loadAnimation(getActivity(),
				android.R.anim.fade_in);
		Animation out = AnimationUtils.loadAnimation(getActivity(),
				android.R.anim.fade_out);
		mTxtAnswer.setInAnimation(in);
		mTxtAnswer.setOutAnimation(out);

		final Handler mHandler = new Handler();
		Thread switchText = new Thread(new Runnable() {

			@Override
			public void run() {
				isTextAnimate = true;
				while (isTextAnimate) {
					try {
						Thread.sleep(1500);
						mHandler.post(new Runnable() {

							@Override
							public void run() {
								mTxtAnswer.setText(questInfo.getDefinition());

							}
						});
						Thread.sleep(1500);
						mHandler.post(new Runnable() {

							@Override
							public void run() {
								mTxtAnswer.setText(questInfo.getAnswer());

							}
						});
					} catch (InterruptedException e) {

					}

				}
			}
		});

		switchText.start();

		ImageView imgRateOne = (ImageView) winDialogView
				.findViewById(R.id.rate_one);
		ImageView imgRateTwo = (ImageView) winDialogView
				.findViewById(R.id.rate_two);
		ImageView imgRateThree = (ImageView) winDialogView
				.findViewById(R.id.rate_three);

		switch (score) {
			case 6:
				imgRateThree.setImageResource(R.drawable.ic_rate_on);
				imgRateTwo.setImageResource(R.drawable.ic_rate_on);
				imgRateOne.setImageResource(R.drawable.ic_rate_on);
				break;
			case 5:
				imgRateThree.setImageResource(R.drawable.ic_rate_half);
				imgRateTwo.setImageResource(R.drawable.ic_rate_on);
				imgRateOne.setImageResource(R.drawable.ic_rate_on);
				break;
			case 4:
				imgRateThree.setImageResource(R.drawable.ic_rate_off);
				imgRateTwo.setImageResource(R.drawable.ic_rate_on);
				imgRateOne.setImageResource(R.drawable.ic_rate_on);
				break;
			case 3:
				imgRateThree.setImageResource(R.drawable.ic_rate_off);
				imgRateTwo.setImageResource(R.drawable.ic_rate_half);
				imgRateOne.setImageResource(R.drawable.ic_rate_on);
				break;
			case 2:
				imgRateThree.setImageResource(R.drawable.ic_rate_off);
				imgRateTwo.setImageResource(R.drawable.ic_rate_off);
				imgRateOne.setImageResource(R.drawable.ic_rate_on);
				break;
			case 1:
				imgRateThree.setImageResource(R.drawable.ic_rate_off);
				imgRateTwo.setImageResource(R.drawable.ic_rate_off);
				imgRateOne.setImageResource(R.drawable.ic_rate_half);
				break;
			default:
				imgRateThree.setImageResource(R.drawable.ic_rate_off);
				imgRateTwo.setImageResource(R.drawable.ic_rate_off);
				imgRateOne.setImageResource(R.drawable.ic_rate_off);
				break;
		}

		ImageView mImgNextBtn = (ImageView) winDialogView
				.findViewById(R.id.btn_start);
		ImageView mImgReplayBtn = (ImageView) winDialogView
				.findViewById(R.id.btn_back);
		mImgNextBtn.setOnClickListener(this);
		mImgReplayBtn.setOnClickListener(this);

		final RelativeLayout mPopupView = (RelativeLayout) winDialogView
				.findViewById(R.id.btn_choose_from_galary);

		class TakeScreenShot extends AsyncTask<Void, Void, Void> {
			AlertDialog replaceDialog;
			ShareUtils.SHARE_TYPE shareType;
			Bitmap b;

			public TakeScreenShot(ShareUtils.SHARE_TYPE shareType) {
				super();
				this.shareType = shareType;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				// Showing progress dialog
				replaceDialog = dialog;
				dialog = new ProgressDialog(getActivity());
				dialog.setMessage(App.getContext().getResources()
						.getString(R.string.please_wait));
				dialog.setCancelable(false);
				dialog.show();
				b = Bitmap.createBitmap(mPopupView.getWidth(),
						mPopupView.getHeight(), Config.ARGB_8888);

				Canvas canvas = new Canvas(b);
				mPopupView.draw(canvas);
			}

			@Override
			protected Void doInBackground(Void... params) {
				File file = new File(ShareUtils.DEFAULT_SCREENSHOT_PATH);
				try {
					file.createNewFile();
					FileOutputStream ostream = new FileOutputStream(file);

					b.compress(CompressFormat.JPEG, 80, ostream);
					ostream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (shareType == SHARE_TYPE.FACEBOOK)
					ShareUtils.postPhotoToFacebook(getActivity(), new File(
							ShareUtils.DEFAULT_SCREENSHOT_PATH));
				else
					ShareUtils.postPhotoToGoogle(getActivity(), new File(
							ShareUtils.DEFAULT_SCREENSHOT_PATH));

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// Dismiss the progress dialog
				if (dialog.isShowing())
					dialog.dismiss();
				dialog = replaceDialog;
			}

		}

		final TakeScreenShot takeScreenShotFb = new TakeScreenShot(
				SHARE_TYPE.FACEBOOK);
		final TakeScreenShot takeScreenShotGg = new TakeScreenShot(
				SHARE_TYPE.GOOGLE);

		ImageView mBtnFacebook = (ImageView) winDialogView
				.findViewById(R.id.btn_custom);
		ImageView mBtnGoogle = (ImageView) winDialogView
				.findViewById(R.id.btn_create);
		mBtnFacebook.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (takeScreenShotFb.getStatus() != AsyncTask.Status.FINISHED)
					takeScreenShotFb.execute();
			}
		});
		mBtnGoogle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (takeScreenShotGg.getStatus() != AsyncTask.Status.FINISHED)
					takeScreenShotGg.execute();
			}
		});

		View bonusView = winDialogView.findViewById(R.id.place_holder);
		if (!isBonus)
			bonusView.setVisibility(View.GONE);

		return winDialogView;
	}

	private void onWinDialogNextClicked() {
		List<QuestInfo> listQuest = App.getListQuestById(mCategoryId);
		boolean isNext = false;
		int size = listQuest.size();
		for (int i = 0; i < size; i++) {
			int nextQuestIndex = i + 1;
			if (listQuest.get(i).getQuestId() == mQuestId
					&& nextQuestIndex < size) {
				isNext = true;
				QuestFragment fragment = QuestFragment.getInstance(listQuest
						.get(nextQuestIndex).getQuestId(), mCategoryId);
				mNaviManager.showPage(fragment, "");
				break;
			}
		}
		if (!isNext)
			mNaviManager.goBack();
	}

	@Override
	public String getTitle() {
		return mScreenTitle;
	}

	private void shareFacebook() {
		isAnimate = true;
		takeScreenShot(SHARE_TYPE.FACEBOOK);
		isAnimate = false;
	}

	private void shareGoogle() {
		isAnimate = true;
		takeScreenShot(SHARE_TYPE.GOOGLE);
		isAnimate = false;
	}

	// remove frog tag (only remove tag on Quest line)
	private void bombQuest() {
		// TODO: animation
		if (numberOfBomb > 0 && !isBombed) {

			// Sound for bomb
			SoundUtils.getInstance().play(SOUND_NAME.BOOM);

			// Animate bomb
			Animation btn_animation_blink = AnimationUtils.loadAnimation(
					getActivity(), R.anim.btn_animation_blink);
			mImgBomb.startAnimation(btn_animation_blink);

			// Decrease number of bombs
			numberOfBomb--;
			isBombed = true;
			reloadBombAndCompassCount();
			QuestInfo questInfo = App.getQuestById(mQuestId, mCategoryId);
			String answer = questInfo.getAnswer();
			for (Tag tag : mListQuest) {
				if (!answer.contains(tag.string)) {
					// TODO: implement animation
					if (tag.view.getVisibility() == View.VISIBLE)
						tag.view.setVisibility(View.GONE);
				}
			}
		} else {
			SoundUtils.getInstance().play(SOUND_NAME.WRONG);
			// Animate on out of bomb
			Animation shake = AnimationUtils.loadAnimation(getActivity(),
					R.anim.shake);
			mImgBomb.startAnimation(shake);
		}
	}

	// fill the answer and each time it cost one compass
	private void compassQuest() {
		if (numberOfCompass > 0 && !isAnimate
				&& numAnswered < mListAnswer.length) {
			SoundUtils.getInstance().play(SOUND_NAME.COMPASS);
			// Animate flag
			Animation btn_animation_blink = AnimationUtils.loadAnimation(
					getActivity(), R.anim.btn_animation_blink);
			mImgFlag.startAnimation(btn_animation_blink);

			// Decrease one compass
			numberOfCompass--;

			// Update bomb and compass count in textview
			reloadBombAndCompassCount();

			// clear all the tag in answer line which hasn't been filled by
			// compassQuest
			resetAnswerLineAfterPosition(numberOfCompassUsed);
			for (Tag tag : mListQuest) {
				if (tag.string.equals(mListAnswer[numberOfCompassUsed].string)) {
					mListAnswer[numberOfCompassUsed].view.setText(tag.string);
					mListAnswer[numberOfCompassUsed].view
							.setVisibility(View.VISIBLE);
					mListAnswer[numberOfCompassUsed].preView = tag.view;

					// Disable view so user can't remove it from answer line
					mListAnswer[numberOfCompassUsed].view.setEnabled(false);
					tag.view.setVisibility(View.INVISIBLE);

					// Increase number of compass used by one
					numberOfCompassUsed++;

					// Increase number of answered
					numAnswered = numberOfCompassUsed;

					// Check if the last tag hasn't been filled, if true show
					// dialog win game
					if (numAnswered == mListAnswer.length)
						onWinGame();
					return;
				}
			}
		} else {
			// Animate on out of flags
			SoundUtils.getInstance().play(SOUND_NAME.WRONG);
			Animation shake = AnimationUtils.loadAnimation(getActivity(),
					R.anim.shake);
			mImgFlag.startAnimation(shake);
		}

	}

	/*
	 * Reset Quest Tag and Answer Tag
	 */
	private void resetAnswerLineAfterPosition(int pos) {
		for (int i = pos; i < mListAnswer.length; i++) {
			if (mListAnswer[i].preView != null) {
				mListAnswer[i].view.setText("");
				mListAnswer[i].view.setVisibility(View.INVISIBLE);
				mListAnswer[i].preView.setVisibility(View.VISIBLE);
				mListAnswer[i].preView = null;
			}
		}
		numAnswered = 0;
	}

	private void resetGame() {
		reloadBombAndCompassCount();

		// Reset state of compass and bomb usage
		numberOfCompassUsed = 0;
		isBombed = false;

		// Reset num of answered
		numAnswered = 0;
		numOfWrongAnswered = 0;

		// Reset all tag to their default state
		for (Tag tag : mListAnswer) {
			if (tag.view.getVisibility() == View.VISIBLE) {
				tag.view.setVisibility(View.INVISIBLE);
				tag.preView.setVisibility(View.VISIBLE);
				tag.view.setEnabled(true);
				tag.preView.setEnabled(true);
				tag.preView = null;
			}
		}
		for (Tag tag : mListQuest) {
			if (tag.view.getVisibility() == View.GONE) {
				tag.view.setVisibility(View.VISIBLE);
			}
		}
	}

	private void reloadBombAndCompassCount() {
		mTxtBombCount.setText("" + numberOfBomb);
		mTxtCompassCount.setText("" + numberOfCompass);
		setNewBombAndCompass(numberOfBomb, numberOfCompass);
	}

	private void setNewBombAndCompass(int newBomb, int newCompass) {
		ConfigPreference.getInstance().setNumberOfBomb(newBomb);
		ConfigPreference.getInstance().SetNumberOfCompass(newCompass);
	}

	@Override
	public void onClick(View v) {
		if (isAnimate)
			return;
		Animation btn_animation_blink = AnimationUtils.loadAnimation(
				getActivity(), R.anim.btn_animation_blink);
		int id = v.getId();
		switch (id) {
			case R.id.btn_share_facebook:
				SoundUtils.getInstance().play(SOUND_NAME.OTHER_BTN);
				shareFacebook();
				break;
			case R.id.btn_share_google:
				SoundUtils.getInstance().play(SOUND_NAME.OTHER_BTN);
				shareGoogle();
				break;
			case R.id.btn_boom:
				mImgBomb.startAnimation(btn_animation_blink);
				bombQuest();
				break;
			case R.id.btn_flag:
				mImgFlag.startAnimation(btn_animation_blink);
				compassQuest();
				break;
			case R.id.btn_start:
				SoundUtils.getInstance().play(SOUND_NAME.OTHER_BTN);
				isTextAnimate = false;
				if (dialog != null)
					dialog.dismiss();
				onWinDialogNextClicked();
				break;
			case R.id.btn_back:
				SoundUtils.getInstance().play(SOUND_NAME.OTHER_BTN);
				isTextAnimate = false;
				if (dialog != null)
					dialog.dismiss();
				resetGame();
				break;
		}
	}

	@Override
	public BTN_LEFT_MODE getButtonLeftMode() {
		return BTN_LEFT_MODE.BACK;
	}

	@Override
	public void onLeftClicked() {
		mNaviManager.goBack();
	}

	@Override
	public BTN_RIGHT_MODE getButtonRightMode() {
		return BTN_RIGHT_MODE.NONE;
	}

	@Override
	public void onRightClicked() {
		// Do nothing
	}

	public class Tag {
		public String string;
		public TextView view;
		public View preView;
	}
}