package com.duowan.mobile.ixiaoshuo.net;

import android.content.Context;
import android.util.Log;
import com.duowan.mobile.ixiaoshuo.pojo.*;
import com.duowan.mobile.ixiaoshuo.utils.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NetService extends BaseNetService {
	private NetService() {}

	private static NetService mInstance;
	public static NetService get() {
		return mInstance;
	}

	/** must init with application startup */
	public static synchronized void init(Context context) {
		if (mInstance != null) return;
		mInstance = new NetService();
		mInstance.doInit(context);
	}

	public List<BookUpdateInfo> getBookUpdateInfo(List<BookOnUpdate> bookList) {
		if (!mNetworkAvailable) return null;
		try {
			HttpPost httpPost = makeHttpPost("/bookshelf/get_chapter_update.do");
			NetUtil.putListToParams(httpPost, bookList, "bookList");
			Respond respond = handleHttpExecute(httpPost);
			if (Respond.isCorrect(respond)) {
				return respond.convert(new TypeReference<List<BookUpdateInfo>>(){});
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public boolean downloadChapterContent(int bookId, int chapterId) {
		if (!mNetworkAvailable) return false;
		HttpEntity entity = null;
		try {
			String params = "bookId=" + bookId + "&chapterId=" + chapterId;
			HttpResponse response = executeHttpGet("/book/get_chapter_content.do", params);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				InputStream ins = new GZIPInputStream(entity.getContent());
				String content = new String(IOUtil.toByteArray(ins));

				if (StringUtil.isNotEmpty(content)) {
					String fileName = String.valueOf(chapterId);
					File chapterFile = new File(Paths.getCacheDirectorySubFolder(bookId), fileName);
					FileOutputStream contentOutput = new FileOutputStream(chapterFile);
					ZipOutputStream zops = new ZipOutputStream(contentOutput);
					zops.putNextEntry(new ZipEntry(fileName));
					zops.write(content.getBytes(Encoding.GBK.getName()));
					zops.close();
					contentOutput.close();
					return true;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} finally {
			closeEntity(entity);
		}
		return false;
	}

	public List<Chapter> getBookNewlyChapters(int bookId, int lastChapterId) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "bookId=" + bookId + "&lastChapterId=" + lastChapterId;
			Respond respond = handleHttpGet("/book/newly_chapter.do", params);
			if (Respond.isCorrect(respond)) {
				return respond.convert(new TypeReference<List<Chapter>>(){});
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public PaginationList<Chapter> getBookChapterList(int bookId, String order, int pageNo, int pageItemCount) {
		return getBookChapterList(bookId, order, pageNo, pageItemCount, "/book/chapterlist.do");
	}
	
	/**
	 * 获取有声书籍章节列表
	 * @param bookId
	 * @param order
	 * @param pageNo
	 * @param pageItemCount
	 * @return
	 */
	public PaginationList<Chapter> getVoiceBookChapterList(int bookId, String order, int pageNo, int pageItemCount) {
		return getBookChapterList(bookId, order, pageNo, pageItemCount, RequestParameters.GET_VOICE_CHAPLIST);
	}

	
	public PaginationList<Chapter> getSimpleBookChapterList(int bookId, String order, int pageNo, int pageItemCount) {
		return getBookChapterList(bookId, order, pageNo, pageItemCount, "/book/chapterlist_simple.do");
	}

	private PaginationList<Chapter> getBookChapterList(int bookId, String order, int pageNo, int pageItemCount, String pageName) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "bookId=" + bookId + "&pageNo=" + pageNo + "&order=" + order + "&pageItemCount=" + pageItemCount;
			Respond respond = handleHttpGet(pageName, params);
			if (Respond.isCorrect(respond)) {
				Log.i(TAG, "resp is: " + respond.getData().toString());
				return respond.convertPaginationList(Chapter.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public Book getBookDetail(int bookId) {
		if (!mNetworkAvailable) return null;
		try {
			Respond respond = handleHttpGet("/book/detail.do", "bookId=" + bookId);
			if (Respond.isCorrect(respond)) return respond.convert(Book.class);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}
	
	public Book getVoiceBookDetail(int bookId) {
		if (!mNetworkAvailable) return null;
		try {
			Respond respond = handleHttpGet("/book_voice/detail.do", "bookId=" + bookId);
			if (Respond.isCorrect(respond)) return respond.convert(Book.class);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public List<Category> getCategories(String type) {
		if (!mNetworkAvailable) return null;
		try {
			Respond respond = handleHttpGet("/book" + NetUtil.bookDomainPrefix(type) + "/get_categories.do", null);
			if (Respond.isCorrect(respond)) {
				return respond.convert(new TypeReference<List<Category>>(){});
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public PaginationList<Book> getBookListByUpdateStatus(String type, int updateStatus, int pageNo, int pageItemCount) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "updateStatus=" + updateStatus + "&pageNo=" + pageNo + "&pageItemCount=" + pageItemCount;
			Respond respond = handleHttpGet("/book" + NetUtil.bookDomainPrefix(type) + "/list_bystatus.do", params);
			if (Respond.isCorrect(respond)) {
				return respond.convertPaginationList(Book.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public PaginationList<Book> getBookListByCategory(String type, int catId, int pageNo, int pageItemCount) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "catId=" + catId + "&pageNo=" + pageNo + "&pageItemCount=" + pageItemCount;
			Respond respond = handleHttpGet("/book" + NetUtil.bookDomainPrefix(type) + "/list_bycategory.do", params);
			if (Respond.isCorrect(respond)) {
				return respond.convertPaginationList(Book.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public PaginationList<Book> getNewlyBookList(String type, int pageNo, int pageItemCount) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "pageNo=" + pageNo + "&pageItemCount=" + pageItemCount;
			Respond respond = handleHttpGet("/book" + NetUtil.bookDomainPrefix(type) + "/list_bynewly.do", params);
			if (Respond.isCorrect(respond)) {
				Log.i(TAG, "respon is:" + respond.getData().toString());
				return respond.convertPaginationList(Book.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public PaginationList<Book> getHottestBookList(String type, int pageNo, int pageItemCount) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "pageNo=" + pageNo + "&pageItemCount=" + pageItemCount;
			Respond respond = handleHttpGet("/book" + NetUtil.bookDomainPrefix(type) + "/list_byhottest.do", params);
			if (Respond.isCorrect(respond)) {
				return respond.convertPaginationList(Book.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public String[] getHotKeyWords() {
		if (!mNetworkAvailable) return null;
		try {
			Respond respond = handleHttpGet("/book/hot_keywords.do", null);
			if (Respond.isCorrect(respond)) {
				String keywords = respond.convert(String.class);
				if(StringUtil.isNotEmpty(keywords)) {
					return keywords.split(",");
				}
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public boolean userFeedBack(String content, String imei) {
		if (!mNetworkAvailable) return false;
		try {
			HttpPost httpPost = makeHttpPost("/user_feedback.do");

			List<NameValuePair> paramList = new ArrayList<NameValuePair>(2);
			paramList.add(new BasicNameValuePair("content", content));
			paramList.add(new BasicNameValuePair("imei", imei));
			httpPost.setEntity(new UrlEncodedFormEntity(paramList));

			Respond respond = handleHttpExecute(httpPost);
			return Respond.isCorrect(respond);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return false;
	}

	public VersionUpdate getVersionUpdateInfo() {
		return getVersionUpdateInfo(versionCode, versionName);
	}

	public VersionUpdate getVersionUpdateInfo(int versionCode, String versionName) {
		if (!mNetworkAvailable) return null;
		try {
			String params = "versionCode=" + versionCode + "&versionName=" + versionName;
			Respond respond = handleHttpGet("/get_update_version.do", params);
			if (Respond.isCorrect(respond)) {
				return respond.convert(VersionUpdate.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	public boolean downloadFile(String url, File file) {
		if (!mNetworkAvailable || !StringUtil.isValidUrl(url)) return false;
		HttpEntity entity = null;
		try {
			HttpResponse response = executeHttp(new HttpGet(url));
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				byte[] bytes = EntityUtils.toByteArray(entity);
				if (bytes.length > 0) {
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(bytes);
					fos.close();
					return true;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} finally {
			closeEntity(entity);
		}
		return false;
	}


	/**
	 * 通过关键词搜索书籍
	 * @param type
	 * @param pageNo
	 * @param pageItemCount
	 * @return
	 */
	public PaginationList<Book> getBookListBySearch(String type, int pageNo, int pageItemCount,String keyWord) {
		if (!mNetworkAvailable) return null;
		try {
			RequestParameters parameters = new RequestParameters();
			parameters.add(RequestParameters.SEARCH_KEY_WORD, keyWord);
			parameters.add(RequestParameters.TYPE, type);
			parameters.add(RequestParameters.PAGENO, pageNo);
			parameters.add(RequestParameters.PAGE_ITEM_COUNT, pageItemCount);
			Respond respond = handleHttpGet(RequestParameters.BOOK_SEARCH, StringUtil.encodeUrl(parameters));
			if (Respond.isCorrect(respond)) {
				Log.i(TAG, "respons is: " + respond.getData().toString());
				return respond.convertPaginationList(Book.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}
	
	public Member getUserByImei(String imei,String deviceMode,String systemVersion) {
		if (!mNetworkAvailable) return null;
		try {
			RequestParameters parameters = new RequestParameters();
			parameters.add(RequestParameters.IMEI, imei);
			parameters.add(RequestParameters.SYS_VERSION, systemVersion);
			parameters.add(RequestParameters.DEVICE_MODE, deviceMode);
			Respond respond = handleHttpGet(RequestParameters.GET_MEMBER_INFO, StringUtil.encodeUrl(parameters));
			if (Respond.isCorrect(respond)) {
				return respond.convert(Member.class);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

}
