package com.team4.uberapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.*;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.util.Date;

/**
 * Created by hectorguo on 2016/11/2
 * Modified by Lin Zhai on 11/17, add hash and token function.
 */
public class UberAppUtil {
    public static String dataToJson(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, data);
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException("IOEXception while mapping object to JSON");
        }
    }

    // Define the BCrypt workload to use when generating password hashes. 10-31 is a valid value.
    private static int workload = 12;

    /**
     * This method can be used to generate a string representing an account password
     * suitable for storing in a database. It will be an OpenBSD-style crypt(3) formatted
     * hash string of length=60
     * The bcrypt workload is specified in the above static variable, a value from 10 to 31.
     * A workload of 12 is a very reasonable safe default as of 2013.
     * This automatically handles secure 128-bit salt generation and storage within the hash.
     * @param passwordPlaintext The account's plaintext password as provided during account creation,
     *			     or when changing an account's password.
     * @return String - a string of length 60 that is the bcrypt hashed password in crypt(3) format.
     */
    public static String hashPassword(String passwordPlaintext) {
        // gensalt's log_rounds parameter determines the complexity
        // the work factor is 2**log_rounds, and the default is 10
        String hashedPassword = BCrypt.hashpw(passwordPlaintext, BCrypt.gensalt(workload));

        return(hashedPassword);
    }

    /**
     * This method can be used to verify a computed hash from a plaintext (e.g. during a login
     * request) with that of a stored hash from a database. The password hash from the database
     * must be passed as the second variable.
     * @param plaintext The account's plaintext password, as provided during a login request
     * @param hashedText The account's stored password hash, retrieved from the authorization database
     * @return boolean - true if the password matches the password of the stored hash, false otherwise
     */
    public static boolean checkPassword(String plaintext, String hashedText) {
        return BCrypt.checkpw(plaintext, hashedText);
    }
    // secret for token
    private static String tokenSecret = "ilovecmu";
    // token ttl
    private static int tokenTTL = 12 * 3600 * 1000;
    /**
     * This method can be used to generate token for use session, the token is encrypted and hashed
     * it contains username and expiration date.
     * session is expired in 12 hours ,
     * @param userId The account's plaintext password, as provided during a login request
     * @return String - generate token with username
     */
    public static String createToken(String userId) {

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(tokenSecret);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
        Claims customClaims = Jwts.claims();
        customClaims.setIssuer("uberapp-team4");
        customClaims.setSubject("uberapp");
        customClaims.setIssuedAt(now);
        customClaims.setExpiration(new Date(System.currentTimeMillis() + tokenTTL));
        customClaims.put("userID", userId);

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder()
                //.setIssuedAt(now)
                //.setSubject("uberappp")
                //.setIssuer("uberapp-team4")
                .setClaims(customClaims)
                //.setExpiration(new Date(System.currentTimeMillis() + tokenTTL))
                .compressWith(CompressionCodecs.DEFLATE)
                .signWith(signatureAlgorithm, signingKey);

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }

    /**
     * This method can be used to get a valid user ID from jwt token, if
     * 1) userID is contained in token
     * 2) token is no expired
     * @param jwtToken a jwt token
     * @return String - userID in token if not expired or null
     */
    public static String validTokenUser(String jwtToken) {
        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(tokenSecret))
                .parseClaimsJws(jwtToken).getBody();
        JsonObject jsonObject = (JsonObject) new JsonParser().parse(claims.toString());

        if ((claims.getExpiration().getTime() > System.currentTimeMillis()) && (jsonObject.get("userID")!=null) ) {
            return jsonObject.get("userID").toString();
        } else {
            return null;
        }
        //System.out.println("ID: " + claims.getId());
        //System.out.println("Subject: " + claims.getSubject());
        //System.out.println("Issuer: " + claims.getIssuer());
        //System.out.println("Expiration: " + claims.getExpiration());
        //System.out.println("userID: " + jsonObject.get("userID"));
        //return true;
    }
}

