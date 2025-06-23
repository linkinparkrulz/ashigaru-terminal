package com.samourai.wallet.api.backend;

import com.samourai.wallet.api.backend.beans.HttpException;

import java.util.Map;

public interface IBackendClient {
  <T> T getJson(String url, Class<T> responseType, Map<String,String> headers) throws HttpException;
  <T> T getJson(String url, Class<T> responseType, Map<String,String> headers, boolean async) throws HttpException;

  <T> T postUrlEncoded(String url, Class<T> responseType, Map<String,String> headers, Map<String, String> body) throws HttpException;
  String postString(String url, Map<String,String> headers, String contentType, String content) throws HttpException;
}
