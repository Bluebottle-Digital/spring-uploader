package io.fruitful.spring.uploader.dto;

import org.apache.commons.fileupload.FileItem;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RequestParser {
    private static final String FILENAME_PARAM = "qqfile";
    private static final String PART_INDEX_PARAM = "qqpartindex";
    private static final String FILE_SIZE_PARAM = "qqtotalfilesize";
    private static final String TOTAL_PARTS_PARAM = "qqtotalparts";
    private static final String UUID_PARAM = "qquuid";
    private static final String PART_FILENAME_PARAM = "qqfilename";
    private static final String PART_FILESIZE_PARAM = "qqfilesize";
    private static final String METHOD_PARAM = "_method";

    private static final String GENERATE_ERROR_PARAM = "generateError";

    private String filename;
    private FileItem uploadItem;
    private boolean generateError;

    private int partIndex = -1;
    private long totalFileSize;
    private Long fileSize;
    private int totalParts;
    private String uuid;
    private String originalFilename;
    private String method;

    private final Map<String, String> customParams = new HashMap<>();


    private RequestParser()
    {
    }

    //2nd param is null unless a MPFR
    public static RequestParser getInstance(HttpServletRequest request, MultipartUploadParser multipartUploadParser) throws Exception
    {
        RequestParser requestParser = new RequestParser();

        if (multipartUploadParser == null)
        {
            if (request.getMethod().equals("POST") && request.getContentType() == null)
            {
                parseXdrPostParams(request, requestParser);
            }
            else
            {
                requestParser.filename = request.getParameter(FILENAME_PARAM);
                parseQueryStringParams(requestParser, request);
            }
        }
        else
        {
            requestParser.uploadItem = multipartUploadParser.getFirstFile();
            requestParser.filename = multipartUploadParser.getFirstFile().getName();

            //params could be in body or query string, depending on Fine Uploader request option properties
            parseRequestBodyParams(requestParser, multipartUploadParser);
            parseQueryStringParams(requestParser, request);
        }

        removeQqParams(requestParser.customParams);

        return requestParser;
    }

    public String getFilename()
    {
        return originalFilename != null ? originalFilename : filename;
    }

    //only non-null for MPFRs
    public FileItem getUploadItem()
    {
        return uploadItem;
    }

    public boolean generateError()
    {
        return generateError;
    }

    public int getPartIndex()
    {
        return partIndex;
    }

    public long getTotalFileSize()
    {
        return totalFileSize;
    }

    public int getTotalParts()
    {
        return totalParts;
    }

    public String getUuid()
    {
        return uuid;
    }

    public String getOriginalFilename()
    {
        return originalFilename;
    }

    public String getMethod()
    {
        return method;
    }

    public Map<String, String> getCustomParams()
    {
        return customParams;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    private static void parseRequestBodyParams(RequestParser requestParser, MultipartUploadParser multipartUploadParser) throws Exception
    {
        if (multipartUploadParser.getParams().get(GENERATE_ERROR_PARAM) != null)
        {
            requestParser.generateError = Boolean.parseBoolean(multipartUploadParser.getParams().get(GENERATE_ERROR_PARAM));
        }

        String partNumStr = multipartUploadParser.getParams().get(PART_INDEX_PARAM);
        if (partNumStr != null)
        {
            requestParser.partIndex = Integer.parseInt(partNumStr);

            requestParser.totalFileSize = Long.parseLong(multipartUploadParser.getParams().get(FILE_SIZE_PARAM));
            requestParser.totalParts = Integer.parseInt(multipartUploadParser.getParams().get(TOTAL_PARTS_PARAM));
            if (multipartUploadParser.getParams().get(PART_FILESIZE_PARAM) != null) {
            	try {
            		requestParser.fileSize = Long.parseLong(multipartUploadParser.getParams().get(PART_FILESIZE_PARAM));
            	} catch (NumberFormatException nfe) {
            		requestParser.fileSize = null;
            	}
            }
        }

        for (Map.Entry<String, String> paramEntry : multipartUploadParser.getParams().entrySet())
        {
            requestParser.customParams.put(paramEntry.getKey(), paramEntry.getValue());
        }

        if (requestParser.uuid == null)
        {
            requestParser.uuid = multipartUploadParser.getParams().get(UUID_PARAM);
        }

        if (requestParser.originalFilename == null)
        {
            requestParser.originalFilename = multipartUploadParser.getParams().get(PART_FILENAME_PARAM);
        }
    }

    private static void parseQueryStringParams(RequestParser requestParser, HttpServletRequest req)
    {
        if (req.getParameter(GENERATE_ERROR_PARAM) != null)
        {
            requestParser.generateError = Boolean.parseBoolean(req.getParameter(GENERATE_ERROR_PARAM));
        }

        String partNumStr = req.getParameter(PART_INDEX_PARAM);
        if (partNumStr != null)
        {
            requestParser.partIndex = Integer.parseInt(partNumStr);
            requestParser.totalFileSize = Long.parseLong(req.getParameter(FILE_SIZE_PARAM));
            requestParser.totalParts = Integer.parseInt(req.getParameter(TOTAL_PARTS_PARAM));
            if (req.getParameter(PART_FILESIZE_PARAM) != null) {
            	try {
            		requestParser.fileSize = Long.parseLong(req.getParameter(PART_FILESIZE_PARAM));
            	} catch (NumberFormatException nfe) {
            		requestParser.fileSize = null;
            	}
            }
        }

        Enumeration<String> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements())
        {
            String paramName = paramNames.nextElement();
            requestParser.customParams.put(paramName, req.getParameter(paramName));
        }

        if (requestParser.uuid == null)
        {
            requestParser.uuid = req.getParameter(UUID_PARAM);
        }

        if (requestParser.method == null)
        {
            requestParser.method = req.getParameter(METHOD_PARAM);
        }

        if (requestParser.originalFilename == null)
        {
            requestParser.originalFilename = req.getParameter(PART_FILENAME_PARAM);
        }
    }

    private static void removeQqParams(Map<String, String> customParams)
    {
        Iterator<Map.Entry<String, String>> paramIterator = customParams.entrySet().iterator();

        while (paramIterator.hasNext())
        {
            Map.Entry<String, String> paramEntry = paramIterator.next();
            if (paramEntry.getKey().startsWith("qq"))
            {
                paramIterator.remove();
            }
        }
    }

    private static void parseXdrPostParams(HttpServletRequest request, RequestParser requestParser) throws Exception
    {
        String queryString = getQueryStringFromRequestBody(request);
        String[] queryParams = queryString.split("&");

        for (String queryParam : queryParams)
        {
            String[] keyAndVal = queryParam.split("=");
            String key = URLDecoder.decode(keyAndVal[0], "UTF-8");
            String value = URLDecoder.decode(keyAndVal[1], "UTF-8");

            if (key.equals(UUID_PARAM))
            {
                requestParser.uuid = value;
            }
            else if (key.equals(METHOD_PARAM))
            {
                requestParser.method = value;
            }
            else
            {
                requestParser.customParams.put(key, value);
            }
        }
    }

    private static String getQueryStringFromRequestBody(HttpServletRequest request) throws Exception
    {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try
        {
            reader = request.getReader();
            char[] chars = new char[128];
            int bytesRead;
            while ( (bytesRead = reader.read(chars)) != -1 )
            {
                content.append(chars, 0, bytesRead);
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }

        return content.toString();
    }
}
