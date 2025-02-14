package com.example.conversationclassifier.electra;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



import lombok.extern.log4j.Log4j2;




public class BertTokenizer implements Tokenizer {
	private Context context;

	public BertTokenizer(Context context) {
		this.context = context;
		init();
	}

	private String vocab_file = "vocab.txt";
	private Map<String, Integer> token_id_map;
	private Map<Integer, String> id_token_map;
	private boolean do_lower_case = true;
	private boolean do_basic_tokenize = true;
	private List<String> never_split = new ArrayList<String>();
	private String unk_token = "[UNK]";
    private String pad_token = "[PAD]";
	private String cls_token = "[CLS]";
	private String mask_token = "[MASK]";
	private boolean tokenize_chinese_chars = true;
	private BasicTokenizer basic_tokenizer;
	private WordpieceTokenizer wordpiece_tokenizer;

	private static final int MAX_LEN = 512;

	public BertTokenizer(Context context,String vocab_file, boolean do_lower_case, boolean do_basic_tokenize, List<String> never_split,
			String unk_token, String sep_token, String pad_token, String cls_token, String mask_token,
			boolean tokenize_chinese_chars) {
		this.context=context;
		this.vocab_file = vocab_file;
		this.do_lower_case = do_lower_case;
		this.do_basic_tokenize = do_basic_tokenize;
		this.never_split = never_split;
		this.unk_token = unk_token;
        this.pad_token = pad_token;
		this.cls_token = cls_token;
		this.mask_token = mask_token;
		this.tokenize_chinese_chars = tokenize_chinese_chars;
		init();
	}


	private void init() {
		try {
			this.token_id_map = load_vocab(context,vocab_file);
		} catch (IOException e) {
		}
		this.id_token_map = new HashMap<Integer, String>();
		for (String key : token_id_map.keySet()) {
			this.id_token_map.put(token_id_map.get(key), key);
		}

		if (do_basic_tokenize) {
			this.basic_tokenizer = new BasicTokenizer(do_lower_case, never_split, tokenize_chinese_chars);
		}
		this.wordpiece_tokenizer = new WordpieceTokenizer(token_id_map, unk_token);
	}

	private Map<String, Integer> load_vocab(Context context,String vocab_file_name) throws IOException {
		InputStream file = context.getAssets().open(vocab_file_name);
		return TokenizerUtils.generateTokenIdMap(file);
	}

	/**
	 * Tokenizes a piece of text into its word pieces.
	 *
	 * This uses a greedy longest-match-first algorithm to perform tokenization
	 * using the given vocabulary.
	 *
	 * For example: input = "unaffable" output = ["un", "##aff", "##able"]
	 *
	 * Args: text: A single token or whitespace separated tokens. This should have
	 * already been passed through `BasicTokenizer`.
	 *
	 * Returns: A list of wordpiece tokens.
	 * 
	 */
	@Override
	public List<String> tokenize(String text) {
		List<String> split_tokens = new ArrayList<String>();
		if (do_basic_tokenize) {
			for (String token : basic_tokenizer.tokenize(text)) {
				for (String sub_token : wordpiece_tokenizer.tokenize(token)) {
					split_tokens.add(sub_token);
				}
			}
		} else {
			split_tokens = wordpiece_tokenizer.tokenize(text);
		}
		return split_tokens;
	}

	public String convert_tokens_to_string(List<String> tokens) {
		// Converts a sequence of tokens (string) in a single string.
		return tokens.stream().map(s -> s.replace("##", "")).collect(Collectors.joining(" "));
	}

	public List<Integer> convert_tokens_to_ids(List<String> tokens) {
		List<Integer> output = new ArrayList<Integer>();
		for (String s : tokens) {
			output.add(token_id_map.get(s));
		}
		return output;
	}

	public int vocab_size() {
		return token_id_map.size();
	}
}
