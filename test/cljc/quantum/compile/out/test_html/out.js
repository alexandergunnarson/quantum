var todoList = React.createClass({
  displayName: "TodoList",
  render:      function () {
    var createItem = function (itemText, index) {
      return React.createElement("li", { key: (index + itemText) }, itemText);
    };
    return React.createElement("ul", null, this.props.items.map(createItem));
  }
});

var todoApp = React.createClass({
  displayName:     "TodoApp",
  getInitialState: function () {
    return {
      items: [],
      text:  ""
    };
  },
  
  handleSubmit:    function (e) {
    var nextText = "";
    var nextItems = this.state.items.concat([this.state.text]);
    e.preventDefault();
    return this.setState({
      items: nextItems,
      text:  nextText
    });
  },
  
  onChange:        function (e) {
    return this.setState({ text: e.target.value });
  },
  render:          function () {
    return React.createElement("div", null, React.createElement("h3", null, "TODO"), React.createElement(todoList, { items: this.state.items }), React.createElement("form", { onSubmit: this.handleSubmit }, React.createElement("input", {
      onChange: this.onChange,
      value:    this.state.text
    }), React.createElement("button", null, ("Add #" + (this.state.items.length + 1)))));
  }
});

React.render(React.createElement(todoApp, null), document.getElementById("container"));