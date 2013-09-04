package com.duowan.mobile.ixiaoshuo.view.finder;

import android.widget.TextView;
import com.duowan.mobile.ixiaoshuo.R;
import com.duowan.mobile.ixiaoshuo.net.NetService;
import com.duowan.mobile.ixiaoshuo.pojo.Book;
import com.duowan.mobile.ixiaoshuo.reader.MainActivity;
import com.duowan.mobile.ixiaoshuo.utils.PaginationList;
import com.duowan.mobile.ixiaoshuo.utils.StringUtil;

public class FinderNewlyBookListView extends FinderBaseListView {

	public FinderNewlyBookListView(MainActivity activity, OnShowListener onShowListener) {
		super(activity, R.id.lsvFinderNewlyBooks, onShowListener);
	}

	@Override
	protected void build() {
		mView = mActivity.getLayoutInflater().inflate(R.layout.finder_book_listview, null);
		mView.setId(mViewId);
	}

	@Override
	protected PaginationList<Book> loadData() {
		return NetService.get().getNewlyBookList(Book.TYPE_TEXT, mPageNo, PAGE_ITEM_COUNT);
	}

	@Override
	protected void setBookTips(TextView txvBookTips, Book book) {
		txvBookTips.setText(StringUtil.getDiffWithNow(book.getLastUpdateTime()));
	}
}
