/* * * * * * * * * * * * * *
*    include elements      *
* * * * * * * * * * * * * */

/* funtion responsible for writing the header of each page */
function header(page) {

	/* define local variables */
	var bright_color = "#FFDC73";
	var dark_color = "#FFBF00";
	var string = "";
	
	string += '<div data-role="header"><center><img alt="Secure Share" src="./img/secure_file_share.png" width="7.5%" align="middle"/><br/><nav>';
	
	/* manage different case depending on the page loaded */
	switch(page) {
	
		case "home" :
			if (readCookie("token") == null) { string += '<a href="#auth"><button style="width: 100px; color: #784901; background-color: ' + dark_color + '">Home</button></a>'; }
			else { string += '<a href="#home"><button style="width: 100px; color: #784901; background-color: ' + dark_color + '">Home</button></a>'; };
			string += '<a href="#about"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">About</button></a>';
			string += '<a href="#contact"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Contact</button></a>';
			break;
			
		case "auth" :
			if (readCookie("token") == null) { string += '<a href="#auth"><button style="width: 100px; color: #784901; background-color: ' + dark_color + '">Home</button></a>'; }
			else { string += '<a href="#home"><button style="width: 100px; color: #784901; background-color: ' + dark_color + '">Home</button></a>'; };
			string += '<a href="#about"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">About</button></a>';
			string += '<a href="#contact"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Contact</button></a>';
			break;
		
		case "about" :
			if (readCookie("token") == null) { string += '<a href="#auth"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Home</button></a>'; }
			else { string += '<a href="#home"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Home</button></a>'; };
			string += '<a href="#about"><button style="width: 100px; color: #784901; background-color: ' + dark_color + '">About</button></a>';
			string += '<a href="#contact"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Contact</button></a>';
			break;
		
		case "contact" :
			if (readCookie("token") == null) { string += '<a href="#auth"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Home</button></a>'; }
			else { string += '<a href="#home"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Home</button></a>'; };
			string += '<a href="#about"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">About</button></a>';
			string += '<a href="#contact"><button style="width: 100px; color: #784901; background-color: ' + dark_color + '">Contact</button></a>';
			break;
		
		default :
			if (readCookie("token") == null) { string += '<a href="#auth"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Home</button></a>'; }
			else { string += '<a href="#home"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Home</button></a>'; };
			string += '<a href="#about"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">About</button></a>';
			string += '<a href="#contact"><button style="width: 100px; color: #784901; background-color: ' + bright_color + '">Contact</button></a>';
	}
	
	string += '<a href="#auth" onclick=\'eraseCookie("token")\'><button style="width: 100px; color: #784901; background-color: #FFDC73">Log out</button></a>'
	string += '</nav></center></div>'
	
	document.write(string);
};

/* funtion responsible for writing the footer of each page */
function footer() {
	var string = '<div data-role="footer"><center><span style="font-weight: normal; color: #784901">&copy Secure Share - Brice Guillaume, Adam Kettani, Mathieu Lapeyre, J&eacuter&eacutemy Parriaud - Georgia Institute of Technology - ECE 6102 Final Project, Spring 2016</span></center></div>';
	document.write(string);
}

/* * * * * * * * * * * * * *
*  end of include elements *
* * * * * * * * * * * * * */


/* * * * * * * * * * * * * *
*     cookies handling     *
* * * * * * * * * * * * * */

function createCookie(name,value,days) {
		if (days) {
			var date = new Date();
			date.setTime(date.getTime() + (days*24*60*60*1000));
			var expires = "; expires=" + date.toGMTString();
		}
		else var expires = "";
		document.cookie = name + "=" + value + expires;
	}

function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0 ; i < ca.length ; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ') c = c.substring(1, c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length, c.length);
	}
	return null;
}

function eraseCookie(name) {
	if (readCookie(name) != null)
	{
		createCookie(name, "", -1);
		location.reload(true);
	}
}

/* * * * * * * * * * * * * *
* end of cookies handling  *
* * * * * * * * * * * * * */


/* * * * * * * * * * * * * * * * * * * *
*     sign-up at first logging-in      *
* * * * * * * * * * * * * * * * * * * */

/* Contact the server */
var protocol_server = "http";
var ip_server = "104.196.55.208";
var port_server = "8080";
var url_server = protocol_server + "://" + ip_server + ":" + port_server + "/";
var max_timeout = 15000;

/* register the user into the database in case she logs in for the first time
Actually this function cannot be part of the main.js file because
it needs to be called before the DOM is completely loaded */
function signup(token_test) {
		
	if (token_test != null)
	{
		$.ajax({
			/* set the post request to the '/signup' url */
			type: "POST",
			url: url_server + "test/signup",
			data: '{ "token" : "' + token_test + '" }',
			dataType: "json",
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : error
			or no answer from the server */
			error: function(data)
			{
				console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			},
		})
		
		/* answer from the server : error
		or no answer from the server */
		.fail(function()
		{
			alert("An error occured when signing-in");
		});
	}
	
};

/* * * * * * * * * * * * * * * * * * * *
* end of sign-up at first logging-in   *
* * * * * * * * * * * * * * * * * * * */