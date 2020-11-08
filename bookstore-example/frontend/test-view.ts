import {customElement, html ,LitElement} from 'lit-element';
import '@vaadin/vaadin-button/vaadin-button';

@customElement('test-view')
export class TestView extends LitElement {
    render() {
        return html`

        <vaadin-button @click=${this.onClick}>Read More</vaadin-button>
      `;
    }

    onClick() {
        console.log('clicked ');

    }
}
